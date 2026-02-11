package pt.pmfdc.appointmentservice.rooms;

import lombok.Builder;

@Builder(toBuilder = true)
public record Room(
        String externalId,
        String name
) {
    public Room {
        if (externalId == null || externalId.isBlank()) {
            throw new IllegalArgumentException("Room externalId must not be blank");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Room name must not be blank");
        }
    }
}