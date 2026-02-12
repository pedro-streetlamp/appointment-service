package pt.pmfdc.appointmentservice.appointments.api;

import java.time.OffsetDateTime;

public record CreateAppointmentRequestDto(
        String patientName,
        String patientEmail,
        String specialty,
        OffsetDateTime startTime,
        OffsetDateTime endTime
) {}