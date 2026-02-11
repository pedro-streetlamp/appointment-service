package pt.pmfdc.appointmentservice.bdd;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import pt.pmfdc.appointmentservice.rooms.Room;
import pt.pmfdc.appointmentservice.rooms.RoomService;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static pt.pmfdc.appointmentservice.jooq.Tables.ROOMS;

@RequiredArgsConstructor
public class RoomSteps {

    private final RoomService roomService;
    private final DSLContext dsl;

    private List<Room> foundRooms;

    @Before
    public void cleanDatabase() {
        dsl.deleteFrom(ROOMS).execute();
    }

    @Given("the following rooms exist:")
    public void theFollowingRoomsExist(DataTable dataTable) {
        List<Map<String, String>> rows = dataTable.asMaps(String.class, String.class);

        for (Map<String, String> row : rows) {
            int inserted = dsl.insertInto(ROOMS)
                    .set(ROOMS.ID, UUID.randomUUID())
                    .set(ROOMS.EXTERNAL_ID, row.get("externalId"))
                    .set(ROOMS.NAME, row.get("name"))
                    .execute();

            assertEquals(1, inserted, "Expected exactly 1 inserted row");
        }
    }

    @When("I fetch all rooms")
    public void iFetchAllRooms() {
        foundRooms = roomService.fetchAllRooms();
    }

    @Then("I should get rooms:")
    public void iShouldGetRooms(DataTable dataTable) {
        List<Map<String, String>> expectedRows = dataTable.asMaps(String.class, String.class);

        assertNotNull(foundRooms);
        assertEquals(expectedRows.size(), foundRooms.size(), "Unexpected number of rooms returned");

        for (int i = 0; i < expectedRows.size(); i++) {
            Map<String, String> expected = expectedRows.get(i);
            Room actual = foundRooms.get(i);

            assertEquals(expected.get("externalId"), actual.externalId());
            assertEquals(expected.get("name"), actual.name());
        }
    }
}