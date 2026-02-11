package pt.pmfdc.appointmentservice.appointments;

import lombok.Builder;

import java.time.OffsetDateTime;
import java.util.UUID;

@Builder(toBuilder = true)
public record Appointment(
        UUID id,
        UUID doctorId,
        UUID roomId,
        String patientName,
        String patientEmail,
        String specialty,
        OffsetDateTime startTime,
        OffsetDateTime endTime,
        AppointmentStatus status
) {
}