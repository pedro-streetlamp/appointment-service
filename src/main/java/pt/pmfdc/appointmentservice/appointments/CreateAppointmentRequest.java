package pt.pmfdc.appointmentservice.appointments;

import java.time.OffsetDateTime;

public record CreateAppointmentRequest(
        String doctorExternalId,
        String roomExternalId,
        String patientName,
        String patientEmail,
        String specialty,
        OffsetDateTime startTime,
        OffsetDateTime endTime
) {
    public CreateAppointmentRequest {
        if (doctorExternalId == null || doctorExternalId.isBlank()) throw new IllegalArgumentException("doctorExternalId must not be blank");
        if (roomExternalId == null || roomExternalId.isBlank()) throw new IllegalArgumentException("roomExternalId must not be blank");
        if (patientName == null || patientName.isBlank()) throw new IllegalArgumentException("patientName must not be blank");
        if (patientEmail == null || patientEmail.isBlank()) throw new IllegalArgumentException("patientEmail must not be blank");
        if (specialty == null || specialty.isBlank()) throw new IllegalArgumentException("specialty must not be blank");
        if (startTime == null) throw new IllegalArgumentException("startTime must not be null");
        if (endTime == null) throw new IllegalArgumentException("endTime must not be null");
        if (!endTime.isAfter(startTime)) throw new IllegalArgumentException("endTime must be after startTime");
    }
}