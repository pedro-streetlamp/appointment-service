package pt.pmfdc.appointmentservice.bdd;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import pt.pmfdc.appointmentservice.doctors.Doctor;
import pt.pmfdc.appointmentservice.doctors.DoctorService;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static pt.pmfdc.appointmentservice.jooq.Tables.DOCTORS;

@RequiredArgsConstructor
public class DoctorSteps {

    private final DoctorService doctorService;
    private final DSLContext dsl;

    private List<Doctor> foundDoctors;

    @Given("the following doctors exist:")
    public void theFollowingDoctorsExist(DataTable dataTable) {
        List<Map<String, String>> rows = dataTable.asMaps(String.class, String.class);

        for (Map<String, String> row : rows) {
            int inserted = dsl.insertInto(DOCTORS)
                    .set(DOCTORS.ID, UUID.randomUUID())
                    .set(DOCTORS.EXTERNAL_ID, row.get("externalId"))
                    .set(DOCTORS.NAME, row.get("name"))
                    .set(DOCTORS.SPECIALTY, row.get("specialty"))
                    .execute();

            assertEquals(1, inserted, "Expected exactly 1 inserted row");
        }
    }

    @When("I search doctors by specialty {string}")
    public void iSearchDoctorsBySpecialty(String specialty) {
        foundDoctors = doctorService.findDoctorsBySpecialty(specialty);
    }

    @Then("I should get doctors:")
    public void iShouldGetDoctors(DataTable dataTable) {
        List<Map<String, String>> expectedRows = dataTable.asMaps(String.class, String.class);

        assertNotNull(foundDoctors);
        assertEquals(expectedRows.size(), foundDoctors.size(), "Unexpected number of doctors returned");

        for (int i = 0; i < expectedRows.size(); i++) {
            Map<String, String> expected = expectedRows.get(i);
            Doctor actual = foundDoctors.get(i);

            assertEquals(expected.get("externalId"), actual.externalId());
            assertEquals(expected.get("name"), actual.name());
            assertEquals(expected.get("specialty"), actual.specialty());
        }
    }
}