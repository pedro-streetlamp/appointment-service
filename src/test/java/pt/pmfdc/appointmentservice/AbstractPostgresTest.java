package pt.pmfdc.appointmentservice;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;

import java.time.Duration;

@Testcontainers
public abstract class AbstractPostgresTest {

    private static final int WIREMOCK_PORT = 8080;

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("appointments")
            .withUsername("test")
            .withPassword("test");

    @Container
    static final GenericContainer<?> DOCTOR_CALENDAR_WIREMOCK = new GenericContainer<>("wiremock/wiremock:3.6.0")
            .withExposedPorts(WIREMOCK_PORT)
            .withCommand("--verbose", "--global-response-templating")
            .withCopyFileToContainer(
                    MountableFile.forHostPath("wiremock/doctor-calendar/mappings"),
                    "/home/wiremock/mappings"
            )
            .withCopyFileToContainer(
                    MountableFile.forHostPath("wiremock/doctor-calendar/__files"),
                    "/home/wiremock/__files"
            )
            .waitingFor(
                    Wait.forHttp("/__admin")
                            .forPort(WIREMOCK_PORT)
                            .forStatusCode(200)
                            .withStartupTimeout(Duration.ofSeconds(30))
            );

    @Container
    static final GenericContainer<?> ROOM_RESERVATION_WIREMOCK = new GenericContainer<>("wiremock/wiremock:3.6.0")
            .withExposedPorts(WIREMOCK_PORT)
            .withCommand("--verbose", "--global-response-templating")
            .withCopyFileToContainer(
                    MountableFile.forHostPath("wiremock/room-reservation/mappings"),
                    "/home/wiremock/mappings"
            )
            .withCopyFileToContainer(
                    MountableFile.forHostPath("wiremock/room-reservation/__files"),
                    "/home/wiremock/__files"
            )
            .waitingFor(
                    Wait.forHttp("/__admin")
                            .forPort(WIREMOCK_PORT)
                            .forStatusCode(200)
                            .withStartupTimeout(Duration.ofSeconds(30))
            );

    @Container
    static final GenericContainer<?> EMAIL_GATEWAY_WIREMOCK = new GenericContainer<>("wiremock/wiremock:3.6.0")
            .withExposedPorts(WIREMOCK_PORT)
            .withCommand("--verbose", "--global-response-templating")
            .withCopyFileToContainer(
                    MountableFile.forHostPath("wiremock/email-gateway/mappings"),
                    "/home/wiremock/mappings"
            )
            .withCopyFileToContainer(
                    MountableFile.forHostPath("wiremock/email-gateway/__files"),
                    "/home/wiremock/__files"
            )
            .waitingFor(
                    Wait.forHttp("/__admin")
                            .forPort(WIREMOCK_PORT)
                            .forStatusCode(200)
                            .withStartupTimeout(Duration.ofSeconds(30))
            );

    private static void ensureContainersStarted() {
        if (!POSTGRES.isRunning()) {
            POSTGRES.start();
        }
        if (!DOCTOR_CALENDAR_WIREMOCK.isRunning()) {
            DOCTOR_CALENDAR_WIREMOCK.start();
        }
        if (!ROOM_RESERVATION_WIREMOCK.isRunning()) {
            ROOM_RESERVATION_WIREMOCK.start();
        }
        if (!EMAIL_GATEWAY_WIREMOCK.isRunning()) {
            EMAIL_GATEWAY_WIREMOCK.start();
        }
    }

    @DynamicPropertySource
    static void registerDataSourceProperties(DynamicPropertyRegistry registry) {
        ensureContainersStarted();

        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");

        registry.add("spring.liquibase.enabled", () -> true);

        registry.add(
                "doctor-calendar.base-url",
                () -> "http://" + DOCTOR_CALENDAR_WIREMOCK.getHost() + ":" + DOCTOR_CALENDAR_WIREMOCK.getMappedPort(WIREMOCK_PORT)
        );
        registry.add(
                "room-reservation.base-url",
                () -> "http://" + ROOM_RESERVATION_WIREMOCK.getHost() + ":" + ROOM_RESERVATION_WIREMOCK.getMappedPort(WIREMOCK_PORT)
        );
        registry.add(
                "email-gateway.base-url",
                () -> "http://" + EMAIL_GATEWAY_WIREMOCK.getHost() + ":" + EMAIL_GATEWAY_WIREMOCK.getMappedPort(WIREMOCK_PORT)
        );
    }
}