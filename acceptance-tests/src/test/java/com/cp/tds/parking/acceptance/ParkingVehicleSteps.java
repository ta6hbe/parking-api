package com.cp.tds.parking.acceptance;

import com.cp.tds.parking.acceptance.dto.PostParkingRequest;
import com.cp.tds.parking.acceptance.dto.PostParkingResponse;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sends parking requests and verifies their status and successful response bodies.
 */
public class ParkingVehicleSteps {

    private final AtHelper helper;

    private final ScenarioContext context;

    public ParkingVehicleSteps(AtHelper helper, ScenarioContext context) {
        this.helper = helper;
        this.context = context;
    }

    @When("a vehicle attempts to park at the default location with:")
    public void parkAtDefaultLocationWithVehicle(DataTable dataTable) {

        PostParkingRequest request =  parsePostParkingRequest(dataTable);
        attemptToPark("/parking", request);
    }

    @When("a vehicle attempts to park at location {long} with:")
    public void parkAtLocationWithVehicle(long locationId, DataTable dataTable) {
        attemptToPark("/parking/" + locationId, parsePostParkingRequest(dataTable));
    }

    @When("a vehicle attempts to park at the default location with JSON:")
    public void parkAtDefaultLocationWithJson(String json) {
        attemptToPark("/parking", json);
    }

    @Then("I get a {int} http response status")
    public void parkingResponseHasStatus(int expectedStatusCode) {
        assertThat(context.parkingAttemptResponse().getStatusCode().value())
                .as("parking attempt response status")
                .isEqualTo(expectedStatusCode);
    }

    @And("post parking response contains:")
    public void responseBodyContainsAValidSessionUUIDTheVehicleRegistrationSpaceNumberAndNonNullEntryTimestamp(DataTable dataTable) {
        var rows = dataTable.asMaps(String.class, String.class);
        assertThat(rows).as("one expected parking response").hasSize(1);
        var expected = rows.getFirst();
        assertThat(expected).as("expected parking response fields")
                .containsOnlyKeys("parkingSessionId", "vehicleReg", "spaceNumber", "timeIn");

        assertThat(expected.get("parkingSessionId"))
                .as("session ID expectation marker")
                .isEqualTo("UUID");
        assertThat(expected.get("timeIn"))
                .as("entry timestamp expectation marker")
                .isEqualTo("TIMESTAMP");

        Object responseBody = context.parkingAttemptResponse().getBody();
        assertThat(responseBody).as("parking response body")
                .isNotNull()
                .isInstanceOf(PostParkingResponse.class);
        PostParkingResponse body = (PostParkingResponse) responseBody;

        assertThat(body.parkingSessionId())
                .as("session ID parsed as a UUID")
                .isNotNull();

        assertThat(body.vehicleReg())
                .as("normalised vehicle registration")
                .isEqualTo(expected.get("vehicleReg"));
        assertThat(body.spaceNumber())
                .as("allocated parking space")
                .isEqualTo(Integer.parseInt(Objects.requireNonNull(expected.get("spaceNumber"))));
        assertThat(body.timeIn())
                .as("entry timestamp parsed as an Instant")
                .isNotNull();
    }

    private void attemptToPark(String path, Object requestBody) {
        context.recordParkingAttemptResponse(helper.attemptToPark(path, requestBody));
    }

    private PostParkingRequest parsePostParkingRequest(DataTable dataTable) {
        var rows = dataTable.asMaps(String.class, String.class);
        assertThat(rows).hasSize(1);

        var row = rows.getFirst();

        return new PostParkingRequest(
                row.get("vehicleReg"),
                Integer.parseInt(Objects.requireNonNull(row.get("vehicleType")))
        );
    }
}
