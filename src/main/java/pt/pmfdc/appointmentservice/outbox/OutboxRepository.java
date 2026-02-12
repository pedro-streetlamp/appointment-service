package pt.pmfdc.appointmentservice.outbox;

import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static pt.pmfdc.appointmentservice.jooq.tables.OutboxMessages.OUTBOX_MESSAGES;

@Repository
@RequiredArgsConstructor
public class OutboxRepository {

    private final DSLContext dsl;

    public UUID insertNew(String aggregateType, UUID aggregateId, String type, String payload) {
        UUID id = UUID.randomUUID();

        int inserted = dsl.insertInto(OUTBOX_MESSAGES)
                .set(OUTBOX_MESSAGES.ID, id)
                .set(OUTBOX_MESSAGES.AGGREGATE_TYPE, aggregateType)
                .set(OUTBOX_MESSAGES.AGGREGATE_ID, aggregateId)
                .set(OUTBOX_MESSAGES.TYPE, type)
                .set(OUTBOX_MESSAGES.PAYLOAD, payload)
                .set(OUTBOX_MESSAGES.STATUS, OutboxStatus.NEW.name())
                .set(OUTBOX_MESSAGES.ATTEMPTS, 0)
                .set(OUTBOX_MESSAGES.NEXT_ATTEMPT_AT, OffsetDateTime.now())
                .execute();

        if (inserted != 1) {
            throw new IllegalStateException("Expected to insert 1 outbox row, but inserted " + inserted);
        }
        return id;
    }

    /**
     * Claim a batch of messages to send. Uses FOR UPDATE SKIP LOCKED to support multiple dispatchers safely.
     */
    public List<OutboxMessageRecord> claimBatch(int batchSize) {
        OffsetDateTime now = OffsetDateTime.now();

        return dsl.transactionResult(cfg -> {
            DSLContext tx = cfg.dsl();

            var rows = tx.selectFrom(OUTBOX_MESSAGES)
                    .where(OUTBOX_MESSAGES.STATUS.in(OutboxStatus.NEW.name(), OutboxStatus.FAILED.name()))
                    .and(OUTBOX_MESSAGES.NEXT_ATTEMPT_AT.le(now))
                    .orderBy(OUTBOX_MESSAGES.CREATED_AT.asc())
                    .limit(batchSize)
                    .forUpdate()
                    .skipLocked()
                    .fetch();

            // mark as SENDING
            for (var r : rows) {
                tx.update(OUTBOX_MESSAGES)
                        .set(OUTBOX_MESSAGES.STATUS, OutboxStatus.SENDING.name())
                        .set(OUTBOX_MESSAGES.UPDATED_AT, OffsetDateTime.now())
                        .where(OUTBOX_MESSAGES.ID.eq(r.getId()))
                        .execute();
            }

            return rows.map(r -> OutboxMessageRecord.builder()
                    .id(r.getId())
                    .aggregateType(r.getAggregateType())
                    .aggregateId(r.getAggregateId())
                    .type(r.getType())
                    .payload(r.getPayload())
                    .status(OutboxStatus.valueOf(r.getStatus()))
                    .attempts(r.getAttempts())
                    .nextAttemptAt(r.getNextAttemptAt())
                    .build());
        });
    }

    public void markSent(UUID messageId) {
        int updated = dsl.update(OUTBOX_MESSAGES)
                .set(OUTBOX_MESSAGES.STATUS, OutboxStatus.SENT.name())
                .set(OUTBOX_MESSAGES.UPDATED_AT, OffsetDateTime.now())
                .where(OUTBOX_MESSAGES.ID.eq(messageId))
                .execute();

        if (updated != 1) {
            throw new IllegalStateException("Expected to update 1 outbox row, but updated " + updated);
        }
    }

    public void markFailed(UUID messageId, int attempts, OffsetDateTime nextAttemptAt, String error) {
        dsl.update(OUTBOX_MESSAGES)
                .set(OUTBOX_MESSAGES.STATUS, OutboxStatus.FAILED.name())
                .set(OUTBOX_MESSAGES.ATTEMPTS, attempts)
                .set(OUTBOX_MESSAGES.NEXT_ATTEMPT_AT, nextAttemptAt)
                .set(OUTBOX_MESSAGES.LAST_ERROR, error)
                .set(OUTBOX_MESSAGES.UPDATED_AT, OffsetDateTime.now())
                .where(OUTBOX_MESSAGES.ID.eq(messageId))
                .execute();
    }

    public Optional<String> findStatus(UUID messageId) {
        return dsl.select(OUTBOX_MESSAGES.STATUS)
                .from(OUTBOX_MESSAGES)
                .where(OUTBOX_MESSAGES.ID.eq(messageId))
                .fetchOptional(OUTBOX_MESSAGES.STATUS);
    }
}