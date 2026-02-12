package pt.pmfdc.appointmentservice.appointments.api;

import java.util.List;

public record AppointmentListResponseDto(
        List<AppointmentResponseDto> items,
        int limit,
        int offset,
        int total
) {}