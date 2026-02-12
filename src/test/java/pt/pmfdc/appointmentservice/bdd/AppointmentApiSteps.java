package pt.pmfdc.appointmentservice.bdd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import pt.pmfdc.appointmentservice.appointments.api.CreateAppointmentRequestDto;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static pt.pmfdc.appointmentservice.bdd.support.JwtTestTokens.adminToken;
import static pt.pmfdc.appointmentservice.bdd.support.JwtTestTokens.nonAdminToken;
import static pt.pmfdc.appointmentservice.jooq.Tables.APPOINTMENTS;

@RequiredArgsConstructor
public class AppointmentApiSteps {

    private final TestRestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final DSLContext dsl;

    @Value("${admin.jwt.secret}")
    private String jwtSecret;

    private ResponseEntity<String> lastResponse;
    private UUID createdAppointmentId;

    // ... existing code ...

    @When("I list appointments via API")
    public void iListAppointmentsViaApi() {
        lastResponse = restTemplate.exchange(
                "/appointments",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                String.class
        );
    }

    @When("I list appointments via API as admin")
    public void iListAppointmentsViaApiAsAdmin() {
        String token = adminToken(jwtSecret);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        lastResponse = restTemplate.exchange(
                "/appointments",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );
    }

    @When("I list appointments via API as non-admin")
    public void iListAppointmentsViaApiAsNonAdmin() {
        String token = nonAdminToken(jwtSecret);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        lastResponse = restTemplate.exchange(
                "/appointments",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );
    }

    @When("I create an appointment via API:")
    public void iCreateAnAppointmentViaApi(DataTable dataTable) throws Exception {
        Map<String, String> row = dataTable.asMaps(String.class, String.class).get(0);

        var body = new CreateAppointmentRequestDto(
                row.get("patientName"),
                row.get("patientEmail"),
                row.get("specialty"),
                OffsetDateTime.parse(row.get("startTime")),
                OffsetDateTime.parse(row.get("endTime"))
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        lastResponse = restTemplate.exchange(
                "/appointments",
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                String.class
        );

        createdAppointmentId = null;
        if (lastResponse.getStatusCode().is2xxSuccessful() && lastResponse.getBody() != null) {
            JsonNode json = objectMapper.readTree(lastResponse.getBody());
            if (json.hasNonNull("id")) {
                createdAppointmentId = UUID.fromString(json.get("id").asText());
            }
        }
    }

    @Then("the API response status should be {int}")
    public void theApiResponseStatusShouldBe(int status) {
        assertNotNull(lastResponse, "Expected an HTTP response");
        assertEquals(status, lastResponse.getStatusCode().value(), "Unexpected HTTP status. Body: " + lastResponse.getBody());
    }

    @Then("I should get an API error message {string}")
    public void iShouldGetAnApiErrorMessage(String expectedMessage) throws Exception {
        assertNotNull(lastResponse);
        assertNotNull(lastResponse.getBody());

        JsonNode json = objectMapper.readTree(lastResponse.getBody());
        assertTrue(json.hasNonNull("message"), "Expected error JSON to have 'message'. Body: " + lastResponse.getBody());
        assertEquals(expectedMessage, json.get("message").asText());
    }

    @Then("the appointment should be created in the database")
    public void theAppointmentShouldBeCreatedInTheDatabase() {
        assertNotNull(createdAppointmentId, "Expected a created appointment id from the API response");
        boolean exists = dsl.fetchExists(
                dsl.selectOne()
                        .from(APPOINTMENTS)
                        .where(APPOINTMENTS.ID.eq(createdAppointmentId))
        );
        assertTrue(exists, "Expected appointment to exist in DB");
    }
}