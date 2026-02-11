package pt.pmfdc.appointmentservice.bdd;

import io.cucumber.java.Before;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;

import static pt.pmfdc.appointmentservice.jooq.Tables.APPOINTMENTS;
import static pt.pmfdc.appointmentservice.jooq.Tables.DOCTORS;
import static pt.pmfdc.appointmentservice.jooq.Tables.ROOMS;

@RequiredArgsConstructor
public class Hooks {

    private final DSLContext dsl;

    @Before
    public void cleanDatabase() {
        dsl.deleteFrom(APPOINTMENTS).execute();
        dsl.deleteFrom(DOCTORS).execute();
        dsl.deleteFrom(ROOMS).execute();
    }
}