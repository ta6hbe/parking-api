package com.cp.tds.parking.acceptance;

import io.cucumber.java.en.When;

/**
 * Requests occupancy for unknown locations through HTTP.
 */
public class ParkingLocationErrorSteps {

    private final AtHelper helper;
    private final ScenarioContext context;

    public ParkingLocationErrorSteps(AtHelper helper, ScenarioContext context) {
        this.helper = helper;
        this.context = context;
    }

    @When("I request parking availability for an unknown location {long}")
    public void iRequestAvailabilityForAnUnknownLocation(long locationId) {
        context.recordAttemptResponse(helper.getOccupancyProblem(locationId));
    }
}
