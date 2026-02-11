package pt.pmfdc.appointmentservice.doctors;

import lombok.Builder;

@Builder(toBuilder = true)
public record Doctor(
        String externalId,
        String name,
        String specialty
) {
    public Doctor {
        if (externalId == null || externalId.isBlank()) {
            throw new IllegalArgumentException("Doctor externalId must not be blank");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Doctor name must not be blank");
        }
        if (specialty == null || specialty.isBlank()) {
            throw new IllegalArgumentException("Doctor specialty must not be blank");
        }
    }
}