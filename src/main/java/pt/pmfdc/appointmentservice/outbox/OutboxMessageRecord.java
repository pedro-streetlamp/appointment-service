package pt.pmfdc.appointmentservice.outbox;

import lombok.Builder;

import java.time.OffsetDateTime;
import java.util.UUID;

@Builder
public record OutboxMessageRecord(
        UUID id,
        String aggregateType,
        UUID aggregateId,
        String type,
        String payload,
        OutboxStatus status,
        int attempts,
        OffsetDateTime nextAttemptAt
) {
}