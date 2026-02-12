package pt.pmfdc.appointmentservice.appointments.api;

import java.util.UUID;

public class AppointmentNotFoundException extends RuntimeException {
    public AppointmentNotFoundException(UUID id) {
        super("Appointment not found: " + id);
    }
}