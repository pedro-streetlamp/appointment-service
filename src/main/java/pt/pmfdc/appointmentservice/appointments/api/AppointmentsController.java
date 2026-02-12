package pt.pmfdc.appointmentservice.appointments.api;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.pmfdc.appointmentservice.appointments.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/appointments")
public class AppointmentsController {

    private final AppointmentService appointmentService;
    private final AppointmentRepository appointmentRepository;

    @PostMapping
    public ResponseEntity<AppointmentResponseDto> createAppointment(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody CreateAppointmentRequestDto body
    ) {
        // Note: idempotencyKey is currently accepted but not persisted/used.
        UUID id = appointmentService.createAppointment(new CreateAppointmentRequest(
                body.patientName(),
                body.patientEmail(),
                body.specialty(),
                body.startTime(),
                body.endTime()
        ));

        var view = appointmentRepository.findViewById(id)
                .orElseThrow(() -> new IllegalStateException("Appointment created but not found: " + id));

        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(view));
    }

    @GetMapping("/{appointmentId}")
    public AppointmentResponseDto getAppointment(@PathVariable UUID appointmentId) {
        var view = appointmentRepository.findViewById(appointmentId)
                .orElseThrow(() -> new AppointmentNotFoundException(appointmentId));
        return toDto(view);
    }

    @GetMapping
    public AppointmentListResponseDto listAppointments(
            @RequestParam(required = false) OffsetDateTime from,
            @RequestParam(required = false) OffsetDateTime to,
            @RequestParam(required = false) String specialty,
            @RequestParam(required = false) String status,
            @RequestParam(required = false, defaultValue = "100") int limit,
            @RequestParam(required = false, defaultValue = "0") int offset
    ) {
        if (limit < 1 || limit > 500) throw new IllegalArgumentException("limit must be between 1 and 500");
        if (offset < 0) throw new IllegalArgumentException("offset must be >= 0");

        String domainStatus = mapApiStatusToDomain(status);

        int total = appointmentRepository.countViews(from, to, specialty, domainStatus);
        List<AppointmentRepository.AppointmentView> items =
                appointmentRepository.listViews(from, to, specialty, domainStatus, limit, offset);

        return new AppointmentListResponseDto(
                items.stream().map(this::toDto).toList(),
                limit,
                offset,
                total
        );
    }

    private AppointmentResponseDto toDto(AppointmentRepository.AppointmentView v) {
        return new AppointmentResponseDto(
                v.id(),
                new AssignedDoctorDto(v.doctorExternalId(), v.doctorName(), v.doctorSpecialty()),
                new AssignedRoomDto(v.roomExternalId(), v.roomName()),
                v.patientName(),
                v.patientEmail(),
                v.specialty(),
                v.startTime(),
                v.endTime(),
                mapDomainStatusToApi(v.status())
        );
    }

    // OpenAPI enum includes CREATED; domain uses PENDING. We translate for contract compliance.
    private String mapDomainStatusToApi(String domainStatus) {
        if (domainStatus == null) return null;
        return domainStatus.equalsIgnoreCase("PENDING") ? "CREATED" : domainStatus.toUpperCase();
    }

    private String mapApiStatusToDomain(String apiStatus) {
        if (apiStatus == null || apiStatus.isBlank()) return null;
        return apiStatus.equalsIgnoreCase("CREATED") ? "PENDING" : apiStatus.toUpperCase();
    }
}