package pt.pmfdc.appointmentservice.email;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class HttpEmailGatewayClient implements EmailGatewayClient {

    private final RestTemplate externalRestTemplate;

    @Value("${email-gateway.base-url}")
    private String baseUrl;

    @Override
    public void send(EmailRequest request) {
        externalRestTemplate.postForEntity(
                baseUrl + "/emails/send",
                new HttpEntity<>(request),
                Void.class
        );
    }
}