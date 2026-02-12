package pt.pmfdc.appointmentservice.appointments;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import pt.pmfdc.appointmentservice.doctors.Doctor;
import pt.pmfdc.appointmentservice.doctors.DoctorService;
import pt.pmfdc.appointmentservice.external.doctor.api.DoctorAppointmentsApi;
import pt.pmfdc.appointmentservice.external.doctor.model.CreateDoctorAppointmentRequest;
import pt.pmfdc.appointmentservice.external.room.api.RoomReservationsApi;
import pt.pmfdc.appointmentservice.external.room.model.CreateRoomReservationRequest;
import pt.pmfdc.appointmentservice.outbox.OutboxDispatcher;
import pt.pmfdc.appointmentservice.outbox.OutboxRepository;
import pt.pmfdc.appointmentservice.rooms.Room;
import pt.pmfdc.appointmentservice.rooms.RoomService;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
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
        long t0 = System.nanoTime();
        log.info("Create appointment started specialty={} startTime={} endTime={}",
                request.specialty(), request.startTime(), request.endTime());

        List<Doctor> doctors = new ArrayList<>(doctorService.findDoctorsBySpecialty(request.specialty()));
        if (doctors.isEmpty()) {
            log.warn("Create appointment failed: no doctors for specialty={}", request.specialty());
            throw new AppointmentCreationException("No doctors found for specialty: " + request.specialty());
        }

        List<Room> rooms = new ArrayList<>(roomService.fetchAllRooms());
        if (rooms.isEmpty()) {
            log.warn("Create appointment failed: no rooms available");
            throw new AppointmentCreationException("No rooms available");
        }

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

                log.info("Appointment persisted id={} status={} doctorId={} roomId={}",
                        appointmentId, AppointmentStatus.PENDING, internalDoctorId, internalRoomId);

                String doctorCalendarAppointmentId = null;
                String roomReservationId = null;

                try {
                    log.debug("Booking doctor calendar id={} doctorExternalId={}", appointmentId, doctor.externalId());
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

                    log.debug("Booking room reservation id={} roomExternalId={}", appointmentId, room.externalId());
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
                        log.info("Appointment confirmed id={}", appointmentId);

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

                        log.debug("Outbox message enqueued type=APPOINTMENT_CONFIRMED_EMAIL aggregateId={}", appointmentId);

                    } catch (Exception dbFailure) {
                        log.error("Failed after external bookings; rolling back id={} (will delete external reservations)", appointmentId, dbFailure);
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

                    long tookMs = (System.nanoTime() - t0) / 1_000_000;
                    log.info("Create appointment succeeded id={} tookMs={}", appointmentId, tookMs);
                    return appointmentId;

                } catch (HttpClientErrorException.Conflict e) {
                    log.info("Slot conflict from external service; retrying other combinations id={}", appointmentId);

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
                    log.error("Create appointment failed id={} (cleanup will run)", appointmentId, e);

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

        log.warn("Create appointment failed: no availability specialty={} startTime={} endTime={}",
                request.specialty(), request.startTime(), request.endTime());
        throw new AppointmentCreationException("No availability (doctor or room) for the requested timeslot");
    }
}