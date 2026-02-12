package pt.pmfdc.appointmentservice.appointments.api;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AppointmentResponseDto(
        UUID id,
        AssignedDoctorDto doctor,
        AssignedRoomDto room,
        String patientName,
        String patientEmail,
        String specialty,
        OffsetDateTime startTime,
        OffsetDateTime endTime,
        String status
) {}