package pt.pmfdc.appointmentservice.appointments.api;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.pmfdc.appointmentservice.appointments.AppointmentCreationException;

import java.util.Map;

@Slf4j
@RestControllerAdvice
public class AppointmentApiExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponseDto> badRequest(IllegalArgumentException ex) {
        log.debug("Validation error: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponseDto("VALIDATION_ERROR", ex.getMessage(), null));
    }

    @ExceptionHandler(AppointmentNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> notFound(AppointmentNotFoundException ex) {
        log.debug("Not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponseDto("NOT_FOUND", ex.getMessage(), null));
    }

    @ExceptionHandler(AppointmentCreationException.class)
    public ResponseEntity<ErrorResponseDto> appointmentCreation(AppointmentCreationException ex) {
        if ("No availability (doctor or room) for the requested timeslot".equals(ex.getMessage())) {
            log.info("No availability: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ErrorResponseDto("NO_AVAILABILITY", ex.getMessage(), null));
        }

        log.warn("Appointment creation error: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponseDto("APPOINTMENT_CREATION_ERROR", ex.getMessage(), null));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> unexpected(Exception ex) {
        log.error("Unexpected error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponseDto("UNEXPECTED_ERROR", "Unexpected error", Map.of("exception", ex.getClass().getSimpleName())));
    }
}