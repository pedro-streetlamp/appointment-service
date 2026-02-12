package pt.pmfdc.appointmentservice.appointments;

import lombok.RequiredArgsConstructor;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.jooq.impl.DSL.trueCondition;
import static pt.pmfdc.appointmentservice.jooq.tables.Appointments.APPOINTMENTS;
import static pt.pmfdc.appointmentservice.jooq.tables.Doctors.DOCTORS;
import static pt.pmfdc.appointmentservice.jooq.tables.Rooms.ROOMS;

@Repository
@RequiredArgsConstructor
public class AppointmentRepository {

    private final DSLContext dsl;

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

    public UUID insert(
            UUID doctorId,
            UUID roomId,
            String patientName,
            String patientEmail,
            String specialty,
            OffsetDateTime startTime,
            OffsetDateTime endTime,
            AppointmentStatus status
    ) {
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

    public void updateStatus(UUID appointmentId, AppointmentStatus status) {
        int updated = dsl.update(APPOINTMENTS)
                .set(APPOINTMENTS.STATUS, status.name())
                .set(APPOINTMENTS.UPDATED_AT, OffsetDateTime.now())
                .where(APPOINTMENTS.ID.eq(appointmentId))
                .execute();

        if (updated != 1) {
            throw new IllegalStateException("Expected to update 1 row, but updated " + updated);
        }
    }

    public void deleteById(UUID appointmentId) {
        dsl.deleteFrom(APPOINTMENTS)
                .where(APPOINTMENTS.ID.eq(appointmentId))
                .execute();
    }

    // ----------------------------
    // Read models for API responses
    // ----------------------------

    public record AppointmentView(
            UUID id,
            String doctorExternalId,
            String doctorName,
            String doctorSpecialty,
            String roomExternalId,
            String roomName,
            String patientName,
            String patientEmail,
            String specialty,
            OffsetDateTime startTime,
            OffsetDateTime endTime,
            String status
    ) {}

    public Optional<AppointmentView> findViewById(UUID appointmentId) {
        return dsl.select(
                        APPOINTMENTS.ID,
                        DOCTORS.EXTERNAL_ID,
                        DOCTORS.NAME,
                        DOCTORS.SPECIALTY,
                        ROOMS.EXTERNAL_ID,
                        ROOMS.NAME,
                        APPOINTMENTS.PATIENT_NAME,
                        APPOINTMENTS.PATIENT_EMAIL,
                        APPOINTMENTS.SPECIALTY,
                        APPOINTMENTS.START_TIME,
                        APPOINTMENTS.END_TIME,
                        APPOINTMENTS.STATUS
                )
                .from(APPOINTMENTS)
                .join(DOCTORS).on(DOCTORS.ID.eq(APPOINTMENTS.DOCTOR_ID))
                .join(ROOMS).on(ROOMS.ID.eq(APPOINTMENTS.ROOM_ID))
                .where(APPOINTMENTS.ID.eq(appointmentId))
                .fetchOptional(r -> new AppointmentView(
                        r.get(APPOINTMENTS.ID),
                        r.get(DOCTORS.EXTERNAL_ID),
                        r.get(DOCTORS.NAME),
                        r.get(DOCTORS.SPECIALTY),
                        r.get(ROOMS.EXTERNAL_ID),
                        r.get(ROOMS.NAME),
                        r.get(APPOINTMENTS.PATIENT_NAME),
                        r.get(APPOINTMENTS.PATIENT_EMAIL),
                        r.get(APPOINTMENTS.SPECIALTY),
                        r.get(APPOINTMENTS.START_TIME),
                        r.get(APPOINTMENTS.END_TIME),
                        r.get(APPOINTMENTS.STATUS)
                ));
    }

    public List<AppointmentView> listViews(
            OffsetDateTime from,
            OffsetDateTime to,
            String specialty,
            String status,
            int limit,
            int offset
    ) {
        Condition c = trueCondition();

        if (from != null) c = c.and(APPOINTMENTS.END_TIME.ge(from));
        if (to != null) c = c.and(APPOINTMENTS.START_TIME.le(to));
        if (specialty != null && !specialty.isBlank()) c = c.and(APPOINTMENTS.SPECIALTY.eq(specialty));
        if (status != null && !status.isBlank()) c = c.and(APPOINTMENTS.STATUS.eq(status));

        return dsl.select(
                        APPOINTMENTS.ID,
                        DOCTORS.EXTERNAL_ID,
                        DOCTORS.NAME,
                        DOCTORS.SPECIALTY,
                        ROOMS.EXTERNAL_ID,
                        ROOMS.NAME,
                        APPOINTMENTS.PATIENT_NAME,
                        APPOINTMENTS.PATIENT_EMAIL,
                        APPOINTMENTS.SPECIALTY,
                        APPOINTMENTS.START_TIME,
                        APPOINTMENTS.END_TIME,
                        APPOINTMENTS.STATUS
                )
                .from(APPOINTMENTS)
                .join(DOCTORS).on(DOCTORS.ID.eq(APPOINTMENTS.DOCTOR_ID))
                .join(ROOMS).on(ROOMS.ID.eq(APPOINTMENTS.ROOM_ID))
                .where(c)
                .orderBy(APPOINTMENTS.START_TIME.asc(), APPOINTMENTS.ID.asc())
                .limit(limit)
                .offset(offset)
                .fetch(r -> new AppointmentView(
                        r.get(APPOINTMENTS.ID),
                        r.get(DOCTORS.EXTERNAL_ID),
                        r.get(DOCTORS.NAME),
                        r.get(DOCTORS.SPECIALTY),
                        r.get(ROOMS.EXTERNAL_ID),
                        r.get(ROOMS.NAME),
                        r.get(APPOINTMENTS.PATIENT_NAME),
                        r.get(APPOINTMENTS.PATIENT_EMAIL),
                        r.get(APPOINTMENTS.SPECIALTY),
                        r.get(APPOINTMENTS.START_TIME),
                        r.get(APPOINTMENTS.END_TIME),
                        r.get(APPOINTMENTS.STATUS)
                ));
    }

    public int countViews(
            OffsetDateTime from,
            OffsetDateTime to,
            String specialty,
            String status
    ) {
        Condition c = trueCondition();

        if (from != null) c = c.and(APPOINTMENTS.END_TIME.ge(from));
        if (to != null) c = c.and(APPOINTMENTS.START_TIME.le(to));
        if (specialty != null && !specialty.isBlank()) c = c.and(APPOINTMENTS.SPECIALTY.eq(specialty));
        if (status != null && !status.isBlank()) c = c.and(APPOINTMENTS.STATUS.eq(status));

        return dsl.fetchCount(
                dsl.selectOne()
                        .from(APPOINTMENTS)
                        .where(c)
        );
    }
}