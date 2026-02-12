package pt.pmfdc.appointmentservice;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import pt.pmfdc.appointmentservice.external.doctor.api.DoctorAppointmentsApi;
import pt.pmfdc.appointmentservice.external.doctor.model.CreateDoctorAppointmentRequest;
import pt.pmfdc.appointmentservice.external.doctor.model.CreateDoctorAppointmentResponse;
import pt.pmfdc.appointmentservice.external.room.api.RoomReservationsApi;
import pt.pmfdc.appointmentservice.external.room.model.CreateRoomReservationRequest;
import pt.pmfdc.appointmentservice.external.room.model.CreateRoomReservationResponse;

import static org.assertj.core.api.Assertions.assertThat;

@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@SpringBootTest
class ExternalClientsGeneratedIT extends AbstractPostgresTest {

    @Autowired
    DoctorAppointmentsApi doctorAppointmentsApi;

    @Autowired
    RoomReservationsApi roomReservationsApi;

    @Test
    void generatedClients_call_wiremock_successfully() {
        var doctorResponse = doctorAppointmentsApi.createDoctorAppointment(
                "DOC-102938",
                new CreateDoctorAppointmentRequest()
                        .externalCorrelationId("6f5b8c2a-9d4c-4b02-9f3a-4c9e5b9c2d11")
                        .startTime(java.time.OffsetDateTime.parse("2026-02-11T10:00:00Z"))
                        .endTime(java.time.OffsetDateTime.parse("2026-02-11T10:30:00Z"))
                        .specialty("DERMATOLOGY")
                        .patientName("Jane Doe"),
                "idem-test-123"
        );

        assertThat(doctorResponse.getStatus()).isEqualTo(CreateDoctorAppointmentResponse.StatusEnum.BOOKED);
        assertThat(doctorResponse.getDoctorId()).isEqualTo("DOC-102938");

        var roomResponse = roomReservationsApi.createRoomReservation(
                "ROOM-12A",
                new CreateRoomReservationRequest()
                .externalCorrelationId("6f5b8c2a-9d4c-4b02-9f3a-4c9e5b9c2d11")
                .startTime(java.time.OffsetDateTime.parse("2026-02-11T10:00:00Z"))
                .endTime(java.time.OffsetDateTime.parse("2026-02-11T10:30:00Z")),
                "idem-test-456"
                );

                assertThat(roomResponse.getStatus()).isEqualTo(CreateRoomReservationResponse.StatusEnum.RESERVED);
        assertThat(roomResponse.getRoomId()).isEqualTo("ROOM-12A");
    }
}