# Decisions & Assumptions

This document captures key decisions, trade-offs, and assumptions for the Appointment Service.
It complements the README by explaining *why* things are the way they are.

## Decisions

### 1) Using the external services as sources of truth
- **Decision:** not keep availability of doctors or rooms because of the concurrent nature of these services.
- **Improvements:** Use events from these services to keep eventually consistent and up-to-date availability information.

### 2) Outbox pattern for email dispatch
- **Decision:** Use an outbox table + scheduled dispatcher to send confirmation emails.
- **Reason:** Improves reliability; avoids losing emails when downstream is temporarily unavailable.
- **Consequences:** Email is eventually consistent (not guaranteed immediate).

## Assumptions

- Assuming fixed length appointments
- Doctors and rooms are available locally via seeded DB data.
