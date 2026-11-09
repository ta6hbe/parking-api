package com.cp.tds.parking.acceptance;

import com.cp.tds.parking.acceptance.dto.PostVehicleParkRequest;
import com.cp.tds.parking.acceptance.dto.PostVehicleParkResponse;
import io.cucumber.java.en.Given;
import org.springframework.http.HttpStatus;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Establishes initial parking occupancy through public HTTP requests.
 */
public class ParkingSetupSteps {

    private final AtHelper helper;
    private final ScenarioContext context;

    public ParkingSetupSteps(AtHelper helper, ScenarioContext context) {
        this.helper = helper;
        this.context = context;
    }

    @Given("parking location {long} has {int} active stay(s)")
    public void parkingLocationHasActiveStays(long locationId, int count) {
        assertThat(count).as("initial stay count for location %s", locationId)
                .isBetween(0, context.expectedCapacity(locationId));
        assertOccupancy(locationId, 0);

        for (int spaceNumber = 1; spaceNumber <= count; spaceNumber++) {
            // Keep the established registrations so duplicate-request scenarios remain reusable.
            String registration = String.format(Locale.ROOT, "L%d%02dCAR", locationId, spaceNumber);
            var response = helper.attemptToPark("/parking/" + locationId,
                    new PostVehicleParkRequest(registration, 1));
            context.recordVehicleParkResponse(response);
            assertThat(response.getStatusCode()).as("initial vehicle is parked through HTTP")
                    .isEqualTo(HttpStatus.CREATED);
            assertThat(response.getBody()).as("initial parking response")
                    .isInstanceOf(PostVehicleParkResponse.class)
                    .extracting("vehicleReg", "spaceNumber")
                    .containsExactly(registration, spaceNumber);
        }

        context.recordExpectedActiveStays(locationId, count);
        assertOccupancy(locationId, count);
    }

    private void assertOccupancy(long locationId, int expectedOccupied) {
        var response = helper.getOccupancy("/parking/" + locationId);
        context.recordAttemptResponse(response);
        assertThat(response.getBody()).as("initial occupancy response")
                .isNotNull()
                .extracting("locationId", "occupiedSpaces", "availableSpaces")
                .containsExactly(locationId, (long) expectedOccupied,
                        (long) context.expectedCapacity(locationId) - expectedOccupied);
    }
}
