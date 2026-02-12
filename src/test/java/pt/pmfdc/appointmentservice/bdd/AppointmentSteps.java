package pt.pmfdc.appointmentservice.bdd;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import pt.pmfdc.appointmentservice.appointments.AppointmentCreationException;
import pt.pmfdc.appointmentservice.appointments.AppointmentService;
import pt.pmfdc.appointmentservice.appointments.CreateAppointmentRequest;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static pt.pmfdc.appointmentservice.jooq.Tables.APPOINTMENTS;

@RequiredArgsConstructor
public class AppointmentSteps {

    private final AppointmentService appointmentService;
    private final DSLContext dsl;

    private UUID createdAppointmentId;
    private Exception lastError;

    @When("I create an appointment:")
    public void iCreateAnAppointment(DataTable dataTable) {
        Map<String, String> row = dataTable.asMaps(String.class, String.class).get(0);

        CreateAppointmentRequest request = new CreateAppointmentRequest(
                row.get("patientName"),
                row.get("patientEmail"),
                row.get("specialty"),
                OffsetDateTime.parse(row.get("startTime")),
                OffsetDateTime.parse(row.get("endTime"))
        );

        try {
            createdAppointmentId = appointmentService.createAppointment(request);
            lastError = null;
        } catch (Exception e) {
            createdAppointmentId = null;
            lastError = e;
        }
    }

    @Then("the appointment should be created successfully")
    public void theAppointmentShouldBeCreatedSuccessfully() {
        assertNull(lastError, "Expected no error but got: " + (lastError == null ? null : lastError.getMessage()));
        assertNotNull(createdAppointmentId);

        boolean exists = dsl.fetchExists(
                dsl.selectOne()
                        .from(APPOINTMENTS)
                        .where(APPOINTMENTS.ID.eq(createdAppointmentId))
        );
        assertTrue(exists, "Expected appointment to exist in DB");
    }

    @Then("I should get an appointment creation error {string}")
    public void iShouldGetAnAppointmentCreationError(String expectedMessage) {
        assertNotNull(lastError, "Expected an error");
        assertInstanceOf(AppointmentCreationException.class, lastError, "Expected AppointmentCreationException but got " + lastError.getClass().getName());
        assertEquals(expectedMessage, lastError.getMessage());
    }
}