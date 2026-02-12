package pt.pmfdc.appointmentservice.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import pt.pmfdc.appointmentservice.email.EmailGatewayClient;
import pt.pmfdc.appointmentservice.email.EmailRequest;

import java.time.Duration;
import java.time.OffsetDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxDispatcher {

    private static final int BATCH_SIZE = 25;

    private final OutboxRepository outboxRepository;
    private final EmailGatewayClient emailGatewayClient;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelayString = "${outbox.dispatcher.fixed-delay-ms:1000}")
    public void scheduledDispatch() {
        dispatchOnce();
    }

    public void dispatchOnce() {
        var batch = outboxRepository.claimBatch(BATCH_SIZE);
        if (!batch.isEmpty()) {
            log.info("Outbox dispatch claimed batchSize={}", batch.size());
        } else {
            log.debug("Outbox dispatch: no messages to process");
        }

        for (var msg : batch) {
            try {
                log.debug("Outbox processing id={} type={} attempts={}", msg.id(), msg.type(), msg.attempts());

                if ("APPOINTMENT_CONFIRMED_EMAIL".equals(msg.type())) {
                    var payload = objectMapper.readValue(msg.payload(), AppointmentConfirmedEmailPayload.class);

                    emailGatewayClient.send(EmailRequest.builder()
                            .to(payload.patientEmail())
                            .subject("Your appointment is confirmed")
                            .body("Hello " + payload.patientName() + ", your appointment is confirmed for " + payload.startTime() + ".")
                            .build());

                    outboxRepository.markSent(msg.id());
                    log.info("Outbox sent id={} type={}", msg.id(), msg.type());
                } else {
                    outboxRepository.markFailed(
                            msg.id(),
                            msg.attempts() + 1,
                            OffsetDateTime.now().plus(Duration.ofHours(1)),
                            "Unknown outbox message type: " + msg.type()
                    );
                    log.warn("Outbox failed (unknown type) id={} type={}", msg.id(), msg.type());
                }
            } catch (Exception e) {
                int attempts = msg.attempts() + 1;
                OffsetDateTime nextAttempt = OffsetDateTime.now().plusSeconds(backoffSeconds(attempts));
                outboxRepository.markFailed(msg.id(), attempts, nextAttempt, safeError(e));

                log.warn("Outbox send failed id={} type={} attempts={} nextAttempt={}",
                        msg.id(), msg.type(), attempts, nextAttempt, e);
            }
        }
    }

    private static long backoffSeconds(int attempts) {
        long seconds = (long) Math.pow(2, Math.min(attempts, 8));
        return Math.min(seconds, 300);
    }

    private static String safeError(Exception e) {
        String message = e.getMessage();
        if (message == null) return e.getClass().getSimpleName();
        String full = e.getClass().getSimpleName() + ": " + message;
        return full.substring(0, Math.min(2000, full.length()));
    }

    public record AppointmentConfirmedEmailPayload(
            String patientName,
            String patientEmail,
            java.time.OffsetDateTime startTime,
            java.time.OffsetDateTime endTime
    ) {}
}