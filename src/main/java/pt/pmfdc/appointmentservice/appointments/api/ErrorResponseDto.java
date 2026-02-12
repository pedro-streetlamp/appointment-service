package pt.pmfdc.appointmentservice.appointments.api;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponseDto(
        String code,
        String message,
        Map<String, Object> details
) {}