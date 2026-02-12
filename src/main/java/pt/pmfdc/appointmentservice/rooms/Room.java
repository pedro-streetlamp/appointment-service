package pt.pmfdc.appointmentservice.rooms;

import lombok.Builder;

import java.util.UUID;

@Builder(toBuilder = true)
public record Room(
        UUID id,
        String externalId,
        String name
) {
    public Room {
        if (id == null) {
            throw new IllegalArgumentException("Room id must not be null");
        }
        if (externalId == null || externalId.isBlank()) {
            throw new IllegalArgumentException("Room externalId must not be blank");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Room name must not be blank");
        }
    }
}