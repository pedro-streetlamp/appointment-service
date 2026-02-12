package pt.pmfdc.appointmentservice.appointments;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import pt.pmfdc.appointmentservice.doctors.Doctor;
import pt.pmfdc.appointmentservice.doctors.DoctorService;
import pt.pmfdc.appointmentservice.external.doctor.api.DoctorAppointmentsApi;
import pt.pmfdc.appointmentservice.external.doctor.model.CreateDoctorAppointmentRequest;
import pt.pmfdc.appointmentservice.external.room.api.RoomReservationsApi;
import pt.pmfdc.appointmentservice.external.room.model.CreateRoomReservationRequest;
import pt.pmfdc.appointmentservice.outbox.OutboxRepository;
import pt.pmfdc.appointmentservice.outbox.OutboxDispatcher;
import pt.pmfdc.appointmentservice.rooms.Room;
import pt.pmfdc.appointmentservice.rooms.RoomService;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final DoctorService doctorService;
    private final RoomService roomService;

    private final DoctorAppointmentsApi doctorAppointmentsApi;
    private final RoomReservationsApi roomReservationsApi;

    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public UUID createAppointment(CreateAppointmentRequest request) {
        List<Doctor> doctors = new ArrayList<>(doctorService.findDoctorsBySpecialty(request.specialty()));
        if (doctors.isEmpty()) {
            throw new AppointmentCreationException("No doctors found for specialty: " + request.specialty());
        }

        List<Room> rooms = new ArrayList<>(roomService.fetchAllRooms());
        if (rooms.isEmpty()) {
            throw new AppointmentCreationException("No rooms available");
        }

        // Randomize to avoid every request hammering "the first" doctor/room under contention
        java.util.Collections.shuffle(doctors, ThreadLocalRandom.current());
        java.util.Collections.shuffle(rooms, ThreadLocalRandom.current());

        for (var doctor : doctors) {
            UUID internalDoctorId = doctor.id();

            for (var room : rooms) {
                UUID internalRoomId = room.id();

                if (appointmentRepository.existsByDoctorSlot(internalDoctorId, request.startTime(), request.endTime())) {
                    continue;
                }
                if (appointmentRepository.existsByRoomSlot(internalRoomId, request.startTime(), request.endTime())) {
                    continue;
                }

                UUID appointmentId = appointmentRepository.insert(
                        internalDoctorId,
                        internalRoomId,
                        request.patientName(),
                        request.patientEmail(),
                        request.specialty(),
                        request.startTime(),
                        request.endTime(),
                        AppointmentStatus.PENDING
                );

                String doctorCalendarAppointmentId = null;
                String roomReservationId = null;

                try {
                    var doctorBooking = doctorAppointmentsApi.createDoctorAppointment(
                            doctor.externalId(),
                            new CreateDoctorAppointmentRequest()
                                    .externalCorrelationId(appointmentId.toString())
                                    .startTime(request.startTime())
                                    .endTime(request.endTime())
                                    .specialty(request.specialty())
                                    .patientName(request.patientName()),
                            "apt-" + appointmentId
                    );
                    doctorCalendarAppointmentId = doctorBooking.getAppointmentId();

                    var roomReservation = roomReservationsApi.createRoomReservation(
                            room.externalId(),
                            new CreateRoomReservationRequest()
                                    .externalCorrelationId(appointmentId.toString())
                                    .startTime(request.startTime())
                                    .endTime(request.endTime()),
                            "apt-" + appointmentId
                    );
                    roomReservationId = roomReservation.getReservationId();

                    try {
                        appointmentRepository.updateStatus(appointmentId, AppointmentStatus.CONFIRMED);

                        String payload = objectMapper.writeValueAsString(
                                new OutboxDispatcher.AppointmentConfirmedEmailPayload(
                                        request.patientName(),
                                        request.patientEmail(),
                                        request.startTime(),
                                        request.endTime()
                                )
                        );

                        outboxRepository.insertNew(
                                "Appointment",
                                appointmentId,
                                "APPOINTMENT_CONFIRMED_EMAIL",
                                payload
                        );

                    } catch (Exception dbFailure) {
                        try {
                            roomReservationsApi.deleteRoomReservation(room.externalId(), roomReservationId);
                        } catch (Exception ignored) {
                        }
                        try {
                            doctorAppointmentsApi.deleteDoctorAppointment(doctor.externalId(), doctorCalendarAppointmentId);
                        } catch (Exception ignored) {
                        }
                        appointmentRepository.deleteById(appointmentId);
                        throw dbFailure;
                    }

                    return appointmentId;

                } catch (HttpClientErrorException.Conflict e) {
                    if (roomReservationId != null) {
                        try {
                            roomReservationsApi.deleteRoomReservation(room.externalId(), roomReservationId);
                        } catch (Exception ignored) {
                        }
                    }
                    if (doctorCalendarAppointmentId != null) {
                        try {
                            doctorAppointmentsApi.deleteDoctorAppointment(doctor.externalId(), doctorCalendarAppointmentId);
                        } catch (Exception ignored) {
                        }
                    }
                    appointmentRepository.deleteById(appointmentId);

                } catch (Exception e) {
                    if (roomReservationId != null) {
                        try {
                            roomReservationsApi.deleteRoomReservation(room.externalId(), roomReservationId);
                        } catch (Exception ignored) {
                        }
                    }
                    if (doctorCalendarAppointmentId != null) {
                        try {
                            doctorAppointmentsApi.deleteDoctorAppointment(doctor.externalId(), doctorCalendarAppointmentId);
                        } catch (Exception ignored) {
                        }
                    }
                    appointmentRepository.deleteById(appointmentId);
                }
            }
        }

        throw new AppointmentCreationException("No availability (doctor or room) for the requested timeslot");
    }
}