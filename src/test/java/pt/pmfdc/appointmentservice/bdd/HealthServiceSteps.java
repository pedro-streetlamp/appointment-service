package pt.pmfdc.appointmentservice.bdd;

import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

public class HealthServiceSteps {

    @Autowired
    private TestRestTemplate restTemplate;

    private ResponseEntity<String> response;

    @When("I call the health endpoint")
    public void i_call_the_health_endpoint() {
        response = restTemplate.getForEntity("/actuator/health", String.class);
    }

    @Then("the response status should be {int}")
    public void the_response_status_should_be(Integer status) {
        assertThat(response.getStatusCode().value()).isEqualTo(status);
    }
}