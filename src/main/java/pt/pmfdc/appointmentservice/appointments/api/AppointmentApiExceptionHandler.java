package pt.pmfdc.appointmentservice.appointments.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.pmfdc.appointmentservice.appointments.AppointmentCreationException;

import java.util.Map;

@RestControllerAdvice
public class AppointmentApiExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponseDto> badRequest(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponseDto("VALIDATION_ERROR", ex.getMessage(), null));
    }

    @ExceptionHandler(AppointmentNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> notFound(AppointmentNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponseDto("NOT_FOUND", ex.getMessage(), null));
    }

    @ExceptionHandler(AppointmentCreationException.class)
    public ResponseEntity<ErrorResponseDto> appointmentCreation(AppointmentCreationException ex) {
        // Map the known “no availability” case to 409.
        if ("No availability (doctor or room) for the requested timeslot".equals(ex.getMessage())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ErrorResponseDto("NO_AVAILABILITY", ex.getMessage(), null));
        }

        // Everything else is a 400 “invalid request” in this contract
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponseDto("APPOINTMENT_CREATION_ERROR", ex.getMessage(), null));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> unexpected(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponseDto("UNEXPECTED_ERROR", "Unexpected error", Map.of("exception", ex.getClass().getSimpleName())));
    }
}