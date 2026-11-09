package com.cp.tds.parking.acceptance;

import com.cp.tds.parking.acceptance.dto.PostVehicleParkRequest;
import com.cp.tds.parking.acceptance.dto.PostVehicleParkResponse;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.time.Instant;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sends parking requests and verifies their status and successful response bodies.
 */
public class VehicleParkingSteps {

    private final AtHelper helper;

    private final ScenarioContext context;

    public VehicleParkingSteps(AtHelper helper, ScenarioContext context) {
        this.helper = helper;
        this.context = context;
    }

    @When("a vehicle attempts to park at the default location with:")
    public void parkAtDefaultLocationWithVehicle(DataTable dataTable) {
        attemptToParkWithTable("/parking", dataTable);
    }

    @When("a vehicle attempts to park at location {long} with:")
    public void parkAtLocationWithVehicle(long locationId, DataTable dataTable) {
        attemptToParkWithTable("/parking/" + locationId, dataTable);
    }

    @When("a vehicle attempts to park at the default location with JSON:")
    public void parkAtDefaultLocationWithJson(String json) {
        attemptToPark("/parking", json);
    }

    @Then("I get a {int} http response status")
    public void parkingResponseHasStatus(int expectedStatusCode) {
        assertThat(context.attemptResponse().getStatusCode().value())
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

        Object responseBody = context.attemptResponse().getBody();
        assertThat(responseBody).as("parking response body")
                .isNotNull()
                .isInstanceOf(PostVehicleParkResponse.class);
        PostVehicleParkResponse body = (PostVehicleParkResponse) responseBody;

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
        context.recordVehicleParkResponse(helper.attemptToPark(path, requestBody));
    }

    private void attemptToParkWithTable(String path, DataTable dataTable) {

        var rows = dataTable.asMaps(String.class, String.class);
        assertThat(rows).hasSize(1);

        var row = rows.getFirst();

        PostVehicleParkRequest request = new PostVehicleParkRequest(
                row.get("vehicleReg"),
                Integer.parseInt(Objects.requireNonNull(row.get("vehicleType")))
        );

        if (row.containsKey("timeIn")) {
            helper.setRequestTime(Instant.parse(Objects.requireNonNull(row.get("timeIn"))));
        }

        attemptToPark(path, request);
    }

    @And("the parking entry time is {string}")
    public void parkingEntryTimeIs(String expectedTime) {
        Object responseBody = context.attemptResponse().getBody();

        assertThat(responseBody)
                .as("parking response body")
                .isInstanceOf(PostVehicleParkResponse.class);

        PostVehicleParkResponse body = (PostVehicleParkResponse) responseBody;

        assertThat(body.timeIn())
                .as("parking entry time")
                .isEqualTo(Instant.parse(expectedTime));
    }
}
