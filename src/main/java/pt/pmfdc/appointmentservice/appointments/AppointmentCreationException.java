package pt.pmfdc.appointmentservice.appointments;

public class AppointmentCreationException extends RuntimeException {
    public AppointmentCreationException(String message) {
        super(message);
    }
}