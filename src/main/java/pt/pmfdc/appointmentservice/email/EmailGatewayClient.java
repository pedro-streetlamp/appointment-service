package pt.pmfdc.appointmentservice.email;

public interface EmailGatewayClient {
    void send(EmailRequest request);
}