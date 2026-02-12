package pt.pmfdc.appointmentservice.email;

import lombok.Builder;

@Builder
public record EmailRequest(
        String to,
        String subject,
        String body
) {
}