package com.cp.tds.parking.acceptance;

import io.cucumber.java.en.Given;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies location existence, capacity, and empty initial state through HTTP.
 */
public class ParkingLocationSteps {

    private final AtHelper helper;
    private final ScenarioContext context;

    public ParkingLocationSteps(AtHelper helper, ScenarioContext context) {
        this.helper = helper;
        this.context = context;
    }

    @Given("the default parking location has a capacity of {int}")
    public void theDefaultParkingLocationHasCapacity(int capacity) {
        parkingLocationHasCapacity(helper.getDefaultLocationId(), capacity);
    }

    @Given("the default parking location starts empty")
    public void theDefaultParkingLocationStartsEmpty() {
        long locationId = helper.getDefaultLocationId();
        var response = helper.getOccupancy("/parking");
        context.recordAttemptResponse(response);
        assertThat(response.getBody()).as("default location occupancy").isNotNull();
        assertThat(response.getBody().locationId()).isEqualTo(locationId);
        assertThat(response.getBody().occupiedSpaces())
                .as("default location starts empty")
                .isZero();
        context.recordExpectedActiveStays(locationId, 0);
    }

    @Given("parking location {long} exists")
    public void parkingLocationExists(long locationId) {
        var response = helper.getOccupancy("/parking/" + locationId);
        context.recordAttemptResponse(response);
        assertThat(response.getBody()).as("location occupancy response").isNotNull();
        assertThat(response.getBody().locationId())
                .as("configured parking location %s", locationId)
                .isEqualTo(locationId);
    }

    @Given("parking location {long} is not configured")
    public void parkingLocationIsNotConfigured(long locationId) {
        var response = helper.getOccupancyProblem(locationId);
        context.recordAttemptResponse(response);
        assertThat(response.getStatusCode().value())
                .as("unknown location is not available through HTTP").isEqualTo(404);
    }

    @Given("parking location {long} has a capacity of {int}")
    public void parkingLocationHasCapacity(long locationId, int capacity) {
        var response = helper.getOccupancy("/parking/" + locationId);
        context.recordAttemptResponse(response);
        assertThat(response.getBody()).as("location occupancy response").isNotNull();
        assertThat(response.getBody().locationId()).isEqualTo(locationId);
        assertThat(response.getBody().availableSpaces() + response.getBody().occupiedSpaces())
                .as("capacity reported for location %s", locationId)
                .isEqualTo((long) capacity);
        context.recordExpectedCapacity(locationId, capacity);
    }
}
