package pt.pmfdc.appointmentservice.rooms;

import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import java.util.List;

import static pt.pmfdc.appointmentservice.jooq.tables.Rooms.ROOMS;

@Repository
@RequiredArgsConstructor
public class RoomRepository {

    private final DSLContext dsl;

    public List<Room> findAll() {
        return dsl.select(ROOMS.ID, ROOMS.EXTERNAL_ID, ROOMS.NAME)
                .from(ROOMS)
                .orderBy(ROOMS.NAME.asc())
                .fetch(record -> new Room(
                        record.get(ROOMS.ID),
                        record.get(ROOMS.EXTERNAL_ID),
                        record.get(ROOMS.NAME)
                ));
    }
}