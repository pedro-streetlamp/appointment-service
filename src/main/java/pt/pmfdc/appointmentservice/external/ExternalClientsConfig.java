package pt.pmfdc.appointmentservice.external;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import pt.pmfdc.appointmentservice.external.doctor.api.DoctorAppointmentsApi;
import pt.pmfdc.appointmentservice.external.doctor.invoker.ApiClient;
import pt.pmfdc.appointmentservice.external.room.api.RoomReservationsApi;

@Configuration
public class ExternalClientsConfig {

    @Bean
    public RestTemplate externalRestTemplate() {
        return new RestTemplate();
    }

    @Bean
    public DoctorAppointmentsApi doctorAppointmentsApi(
            RestTemplate externalRestTemplate,
            @Value("${doctor-calendar.base-url}") String baseUrl
    ) {
        ApiClient apiClient = new ApiClient(externalRestTemplate);
        apiClient.setBasePath(baseUrl);
        return new DoctorAppointmentsApi(apiClient);
    }

    @Bean
    public RoomReservationsApi roomReservationsApi(
            RestTemplate externalRestTemplate,
            @Value("${room-reservation.base-url}") String baseUrl
    ) {
        pt.pmfdc.appointmentservice.external.room.invoker.ApiClient apiClient =
                new pt.pmfdc.appointmentservice.external.room.invoker.ApiClient(externalRestTemplate);
        apiClient.setBasePath(baseUrl);
        return new RoomReservationsApi(apiClient);
    }
}