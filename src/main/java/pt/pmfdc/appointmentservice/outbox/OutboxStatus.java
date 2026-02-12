package pt.pmfdc.appointmentservice.outbox;

public enum OutboxStatus {
    NEW,
    SENDING,
    SENT,
    FAILED
}