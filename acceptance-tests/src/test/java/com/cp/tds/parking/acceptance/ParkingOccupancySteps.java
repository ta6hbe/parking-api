package com.cp.tds.parking.acceptance;

import com.cp.tds.parking.acceptance.dto.OccupancyResponse;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises the occupancy HTTP endpoints and checks their responses in Cucumber scenarios.
 */
public class ParkingOccupancySteps {

    private final AtHelper helper;
    private final ScenarioContext context;
    private Long selectedLocationId;
    private ResponseEntity<OccupancyResponse> response;

    public ParkingOccupancySteps(AtHelper helper, ScenarioContext context) {
        this.helper = helper;
        this.context = context;
    }

    @When("I request its parking availability")
    public void iRequestItsParkingAvailability() {
        selectedLocationId = helper.getDefaultLocationId();
        requestAvailability("/parking");
    }

    @When("I request parking availability for location {long}")
    public void iRequestParkingAvailabilityForLocation(long locationId) {
        selectedLocationId = locationId;
        requestAvailability("/parking/" + locationId);

        assertThat(response.getBody())
                .as("occupancy response body")
                .isNotNull();
        assertThat(response.getBody().locationId())
                .as("response identifies the requested location")
                .isEqualTo(locationId);
    }

    @Then("the number of available spaces should match its maximum capacity")
    public void availableSpacesShouldMatchMaximumCapacity() {
        assertThat(selectedLocationId)
                .as("location selected by the scenario")
                .isNotNull();
        thereShouldBeAvailableSpaces(context.expectedCapacity(selectedLocationId));
    }

    @Then("the number of available spaces should match its maximum capacity minus the active stays")
    public void availableSpacesShouldMatchCapacityMinusActiveStays() {
        assertThat(selectedLocationId)
                .as("location selected by the scenario")
                .isNotNull();
        int expectedAvailable = context.expectedCapacity(selectedLocationId)
                - context.expectedActiveStays(selectedLocationId);
        thereShouldBeAvailableSpaces(expectedAvailable);
    }

    @Then("there should be {int} available spaces")
    public void thereShouldBeAvailableSpaces(int expected) {
        assertThat(response.getBody())
                .as("occupancy response body")
                .isNotNull();
        assertThat(response.getBody().availableSpaces())
                .as("available parking spaces")
                .isEqualTo(expected);
    }

    @Then("there should be {int} occupied spaces")
    public void thereShouldBeOccupiedSpaces(int expected) {
        assertThat(response.getBody())
                .as("occupancy response body")
                .isNotNull();
        assertThat(response.getBody().occupiedSpaces())
                .as("occupied parking spaces")
                .isEqualTo(expected);
    }

    @Then("the response should include an observation timestamp")
    public void responseShouldIncludeAnObservationTimestamp() {
        assertThat(response.getBody())
                .as("occupancy response body")
                .isNotNull();
        assertThat(response.getBody().timeChecked())
                .as("observation timestamp parsed as an Instant")
                .isNotNull();
    }

    @Then("parking availability matches the successful concurrent bookings")
    public void availabilityMatchesSuccessfulConcurrentBookings() {
        List<Long> locationIds = context.concurrentParkingAttempts().stream()
                .map(attempt -> attempt.locationId()).distinct().toList();
        for (long locationId : locationIds) {
            long successfulBookings = context.concurrentParkingAttempts().stream()
                    .filter(attempt -> attempt.locationId() == locationId)
                    .filter(attempt -> attempt.response().getStatusCode().equals(HttpStatus.CREATED)).count();
            long expectedOccupied = context.expectedActiveStays(locationId) + successfulBookings;
            int capacity = context.expectedCapacity(locationId);

            // Expectations come from scenario inputs and HTTP results, never repository counts.
            assertThat(expectedOccupied).as("location %s stays within capacity", locationId)
                    .isBetween(0L, (long) capacity);
            requestAvailability("/parking/" + locationId);
            assertThat(response.getBody()).as("location %s occupancy response", locationId).isNotNull();
            assertThat(response.getBody().locationId()).isEqualTo(locationId);
            assertThat(response.getBody().occupiedSpaces()).isEqualTo(expectedOccupied);
            assertThat(response.getBody().availableSpaces()).isEqualTo(capacity - expectedOccupied);
            responseShouldIncludeAnObservationTimestamp();
        }
    }

    private void requestAvailability(String path) {
        response = helper.getOccupancy(path);
        context.recordAttemptResponse(response);

        assertThat(response.getStatusCode())
                .as("occupancy response status")
                .isEqualTo(HttpStatus.OK);
    }
}
