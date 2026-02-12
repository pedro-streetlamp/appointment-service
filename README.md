# Appointment Service

Spring Boot (Java 17) service for creating and listing medical appointments.  
It assigns an available doctor and room for a requested time slot, persists the appointment in Postgres, and uses an outbox + scheduled dispatcher to send a confirmation email via an email gateway.

See also: [DECISIONS.md](DECISIONS.md) (key decisions / assumptions).

---

## Project structure

High-level layout:

- `src/main/java/pt/pmfdc/appointmentservice`
    - `AppointmentServiceApplication`  
      Spring Boot entry point (also enables scheduling for the outbox dispatcher).
    - `appointments/`  
      Core appointment domain:
        - `AppointmentService` – orchestration (choose doctor/room, call externals, persist, enqueue outbox).
        - `AppointmentRepository` – jOOQ-based persistence and read models for API.
        - `AppointmentStatus` – domain status enum.
        - `appointments/api/` – REST API layer (DTOs, controller, exception handler).
    - `doctors/`, `rooms/`  
      Internal services/repositories used to fetch doctors/rooms (and seeded data).
    - `external/`  
      Generated OpenAPI clients for external systems (doctor calendar, room reservation).
    - `email/`  
      Email gateway client (`EmailGatewayClient`, `HttpEmailGatewayClient`).
    - `outbox/`  
      Outbox table access + dispatcher (`OutboxDispatcher`) that sends emails asynchronously.
    - `JooqConfig`  
      jOOQ wiring.
- `src/main/resources`
    - `application.properties` – local dev config (ports, DB, external base URLs, logging).
    - `db/changelog/**` – Liquibase migrations (tables, seeds, outbox).
- `api/openapi`
    - `appointment-service.openapi.yaml` – this service’s API contract
    - `external/*.openapi.yaml` – external service contracts used for client generation
- `wiremock/*`  
  WireMock stubs (mappings + files) used by Docker Compose for local dev.
- `docker-compose.yml`  
  Starts Postgres + WireMock services + the app container.
- `Dockerfile`  
  Multi-stage build that produces and runs the Spring Boot jar.
- `pom.xml`  
  Maven build, Liquibase+jOOQ integration, OpenAPI client generation.

> Note: OpenAPI clients are generated into `target/generated-sources/openapi/**` during the Maven build.

---

## Services & ports (local)

When running with Docker Compose:

- Appointment Service: `http://localhost:8080`
- Postgres: `localhost:5432` (DB: `appointments`)
- WireMock (doctor calendar): `http://localhost:8081`
- WireMock (room reservation): `http://localhost:8082`
- WireMock (email gateway): `http://localhost:8083`

---

## Running locally

This starts Postgres + WireMocks + the appointment service container:
```bash
docker compose up --build
```

Then call the API at:

- `http://localhost:8080/appointments`

To stop:
```bash
docker compose down
```

To reset the DB volume (removes all data):
```bash
docker compose down -v
```


---

## Admin endpoint authentication (local JWT)

The admin listing endpoint is protected:

- `GET /appointments` requires a valid **Bearer JWT** with an **`admin: true`** claim.
- Other endpoints are not protected by JWT.

### Configure the local JWT secret

The service validates HS256 tokens using `admin.jwt.secret`.

Recommended (so you don't commit secrets): set an env var and reference it from config, e.g.:
```

bash export ADMIN_JWT_SECRET="<YOUR_LOCAL_SECRET>"
```

Then run the service normally (it will pick up the secret via Spring configuration if you wire it that way).

 
### Call the endpoint

Send the token in the `Authorization` header:
```
bash curl -i
-H "Authorization: Bearer <JWT_WITH_admin_true>"
"http://localhost:8080/appointments"
```

---
## Building & tests

Build (skipping tests):
```bash
mvn clean package -DskipTests
```

Run tests:
```bash
mvn test
```

This project includes Cucumber BDD tests under:
- `src/test/java/**/bdd`
- `src/test/resources/features`

---

## Logging

Logging is configured via Spring Boot properties in `src/main/resources/application.properties`.  
Typical setup:

- root at `INFO`
- application package at `DEBUG`

---

## API contract

- Service API: `api/openapi/appointment-service.openapi.yaml`
- External contracts (used for generated clients):
    - `api/openapi/external/doctor-calendar.openapi.yaml`
    - `api/openapi/external/room-reservation.openapi.yaml`

