package pt.pmfdc.appointmentservice.doctors;

import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import java.util.List;

import static pt.pmfdc.appointmentservice.jooq.tables.Doctors.DOCTORS;

@Repository
@RequiredArgsConstructor
public class DoctorRepository {

    private final DSLContext dsl;

    public List<Doctor> findBySpecialty(String specialty) {
        return dsl.select(DOCTORS.EXTERNAL_ID, DOCTORS.NAME, DOCTORS.SPECIALTY)
                .from(DOCTORS)
                .where(DOCTORS.SPECIALTY.eq(specialty))
                .orderBy(DOCTORS.NAME.asc())
                .fetch(record -> new Doctor(
                        record.get(DOCTORS.EXTERNAL_ID),
                        record.get(DOCTORS.NAME),
                        record.get(DOCTORS.SPECIALTY)
                ));
    }
}