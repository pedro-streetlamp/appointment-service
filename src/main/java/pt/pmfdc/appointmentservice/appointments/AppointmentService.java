package pt.pmfdc.appointmentservice.appointments;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;

    @Transactional
    public UUID createAppointment(CreateAppointmentRequest request) {
        UUID doctorId = appointmentRepository.findDoctorIdByExternalId(request.doctorExternalId())
                .orElseThrow(() -> new AppointmentCreationException("Doctor not found: " + request.doctorExternalId()));

        UUID roomId = appointmentRepository.findRoomIdByExternalId(request.roomExternalId())
                .orElseThrow(() -> new AppointmentCreationException("Room not found: " + request.roomExternalId()));

        if (appointmentRepository.existsByDoctorSlot(doctorId, request.startTime(), request.endTime())) {
            throw new AppointmentCreationException("Doctor is already booked for that time slot");
        }
        if (appointmentRepository.existsByRoomSlot(roomId, request.startTime(), request.endTime())) {
            throw new AppointmentCreationException("Room is already booked for that time slot");
        }

        return appointmentRepository.insert(
                doctorId,
                roomId,
                request.patientName(),
                request.patientEmail(),
                request.specialty(),
                request.startTime(),
                request.endTime(),
                AppointmentStatus.CREATED
        );
    }
}