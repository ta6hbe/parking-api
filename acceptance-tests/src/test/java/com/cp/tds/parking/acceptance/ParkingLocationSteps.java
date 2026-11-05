package com.cp.tds.parking.acceptance;

import com.cp.tds.parking.api.domain.ParkingLocation;
import com.cp.tds.parking.api.domain.ParkingLocationStatus;
import io.cucumber.java.en.Given;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies configured locations, capacities, and empty initial state.
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
        parkingLocationHasCapacity(helper.getParkingProperties().getDefaultLocationId(), capacity);
    }

    @Given("the default parking location starts empty")
    public void theDefaultParkingLocationStartsEmpty() {
        long locationId = helper.getParkingProperties().getDefaultLocationId();
        helper.configuredLocation(locationId);
        assertThat(helper.getActiveStays().countByLocationId(locationId))
                .as("default location starts empty")
                .isZero();
        context.recordExpectedActiveStays(locationId, 0);
    }

    @Given("parking location {long} exists")
    public void parkingLocationExists(long locationId) {
        assertThat(helper.configuredLocation(locationId).id())
                .as("configured parking location %s", locationId)
                .isEqualTo(locationId);
    }

    @Given("parking location {long} is closed")
    public void parkingLocationIsClosed(long locationId) {
        assertThat(helper.configuredLocation(locationId).status())
                .as("location %s is closed to arrivals", locationId)
                .isEqualTo(ParkingLocationStatus.CLOSED);
    }

    @Given("parking location {long} is not configured")
    public void parkingLocationIsNotConfigured(long locationId) {
        assertThat(helper.getParkingProperties().getLocations())
                .as("configured location IDs")
                .extracting(ParkingLocation::id)
                .doesNotContain(locationId);
    }

    @Given("parking location {long} has a capacity of {int}")
    public void parkingLocationHasCapacity(long locationId, int capacity) {
        assertThat(helper.configuredLocation(locationId).maxCapacity())
                .as("configured capacity of location %s", locationId)
                .isEqualTo(capacity);
    }


}
