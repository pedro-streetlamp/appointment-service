package pt.pmfdc.appointmentservice.bdd;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;
import pt.pmfdc.appointmentservice.appointments.AppointmentService;
import pt.pmfdc.appointmentservice.appointments.CreateAppointmentRequest;
import pt.pmfdc.appointmentservice.outbox.OutboxDispatcher;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@RequiredArgsConstructor
public class AppointmentEmailOutboxSteps {

    private final AppointmentService appointmentService;
    private final OutboxDispatcher outboxDispatcher;

    // Reuse the same RestTemplate style as the app uses for external calls.
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${email-gateway.base-url}")
    private String emailGatewayBaseUrl;

    private UUID createdAppointmentId;
    private Exception lastError;

    @When("I create an appointment to trigger email:")
    public void iCreateAnAppointmentToTriggerEmail(DataTable dataTable) {
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

        assertThat(lastError)
                .as("Appointment creation should succeed for this scenario")
                .isNull();

        assertThat(createdAppointmentId).isNotNull();
    }

    @Then("when I dispatch the outbox once, an email should be sent to {string}")
    public void whenIDispatchTheOutboxOnceAnEmailShouldBeSentTo(String expectedRecipient) {
        outboxDispatcher.dispatchOnce();

        int count = countEmailRequestsToRecipient(expectedRecipient);
        assertThat(count)
                .as("Expected exactly 1 email request to be sent to " + expectedRecipient)
                .isEqualTo(1);
    }

    private int countEmailRequestsToRecipient(String recipient) {
        String url = emailGatewayBaseUrl + "/__admin/requests/count";

        // WireMock request count API:
        // https://wiremock.org/docs/admin-api/#count-requests-matching-a-pattern
        String body = """
                {
                  "method": "POST",
                  "urlPath": "/emails/send",
                  "bodyPatterns": [
                    { "matchesJsonPath": "$[?(@.to == '%s')]" }
                  ]
                }
                """.formatted(recipient);

        @SuppressWarnings("unchecked")
        Map<String, Object> response = restTemplate.postForObject(
                url,
                org.springframework.http.RequestEntity
                        .post(java.net.URI.create(url))
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(body),
                Map.class
        );

        assertThat(response).isNotNull();
        Object count = response.get("count");
        assertThat(count).as("WireMock count response should contain 'count'").isNotNull();

        return ((Number) count).intValue();
    }
}