package pt.pmfdc.appointmentservice.appointments.api;

public record AssignedDoctorDto(
        String externalId,
        String name,
        String specialty
) {}