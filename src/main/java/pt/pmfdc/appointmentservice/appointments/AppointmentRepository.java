package pt.pmfdc.appointmentservice.appointments;

import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static pt.pmfdc.appointmentservice.jooq.tables.Appointments.APPOINTMENTS;
import static pt.pmfdc.appointmentservice.jooq.tables.Doctors.DOCTORS;
import static pt.pmfdc.appointmentservice.jooq.tables.Rooms.ROOMS;

@Repository
@RequiredArgsConstructor
public class AppointmentRepository {

    private final DSLContext dsl;

    public Optional<UUID> findDoctorIdByExternalId(String externalId) {
        return dsl.select(DOCTORS.ID)
                .from(DOCTORS)
                .where(DOCTORS.EXTERNAL_ID.eq(externalId))
                .fetchOptional(DOCTORS.ID);
    }

    public Optional<UUID> findRoomIdByExternalId(String externalId) {
        return dsl.select(ROOMS.ID)
                .from(ROOMS)
                .where(ROOMS.EXTERNAL_ID.eq(externalId))
                .fetchOptional(ROOMS.ID);
    }

    public boolean existsByDoctorSlot(UUID doctorId, OffsetDateTime startTime, OffsetDateTime endTime) {
        return dsl.fetchExists(
                dsl.selectOne()
                        .from(APPOINTMENTS)
                        .where(APPOINTMENTS.DOCTOR_ID.eq(doctorId))
                        .and(APPOINTMENTS.START_TIME.eq(startTime))
                        .and(APPOINTMENTS.END_TIME.eq(endTime))
        );
    }

    public boolean existsByRoomSlot(UUID roomId, OffsetDateTime startTime, OffsetDateTime endTime) {
        return dsl.fetchExists(
                dsl.selectOne()
                        .from(APPOINTMENTS)
                        .where(APPOINTMENTS.ROOM_ID.eq(roomId))
                        .and(APPOINTMENTS.START_TIME.eq(startTime))
                        .and(APPOINTMENTS.END_TIME.eq(endTime))
        );
    }

    public UUID insert(UUID doctorId,
                       UUID roomId,
                       String patientName,
                       String patientEmail,
                       String specialty,
                       OffsetDateTime startTime,
                       OffsetDateTime endTime,
                       AppointmentStatus status) {

        UUID id = UUID.randomUUID();

        int inserted = dsl.insertInto(APPOINTMENTS)
                .set(APPOINTMENTS.ID, id)
                .set(APPOINTMENTS.DOCTOR_ID, doctorId)
                .set(APPOINTMENTS.ROOM_ID, roomId)
                .set(APPOINTMENTS.PATIENT_NAME, patientName)
                .set(APPOINTMENTS.PATIENT_EMAIL, patientEmail)
                .set(APPOINTMENTS.SPECIALTY, specialty)
                .set(APPOINTMENTS.START_TIME, startTime)
                .set(APPOINTMENTS.END_TIME, endTime)
                .set(APPOINTMENTS.STATUS, status.name())
                .execute();

        if (inserted != 1) {
            throw new IllegalStateException("Expected to insert 1 row, but inserted " + inserted);
        }

        return id;
    }
}