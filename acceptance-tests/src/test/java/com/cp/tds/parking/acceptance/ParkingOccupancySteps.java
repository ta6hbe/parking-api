package com.cp.tds.parking.acceptance;

import com.cp.tds.parking.api.config.ParkingProperties;
import com.cp.tds.parking.api.domain.ParkingLocation;
import com.cp.tds.parking.api.domain.ParkingStay;
import com.cp.tds.parking.api.domain.VehicleType;
import com.cp.tds.parking.api.persistence.ActiveParkingStayRepositoryI;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises the occupancy HTTP endpoints and checks their responses in Cucumber scenarios.
 */
public class ParkingOccupancySteps {

    private final ParkingProperties parkingProperties;
    private final RestClient client;
    private final ActiveParkingStayRepositoryI activeStays;
    private final Clock clock;
    private final Map<Long, Integer> expectedActiveStays = new HashMap<>();

    private ParkingLocation selectedLocation;
    private ResponseEntity<OccupancyResponse> response;

    public ParkingOccupancySteps(
            Environment environment,
            ParkingProperties parkingProperties,
            ActiveParkingStayRepositoryI activeStays,
            Clock clock) {
        this.parkingProperties = parkingProperties;
        this.activeStays = activeStays;
        this.clock = clock;

        int port = environment.getRequiredProperty("server.port", Integer.class);
        this.client = RestClient.create("http://localhost:" + port);
    }

    @Given("the default parking location has a capacity of {int}")
    public void theDefaultParkingLocationHasCapacity(int capacity) {
        selectedLocation = configuredLocation(parkingProperties.getDefaultLocationId());
        assertThat(selectedLocation.maxCapacity())
                .as("configured default parking capacity")
                .isEqualTo(capacity);
    }

    @Given("parking location {long} exists")
    public void parkingLocationExists(long locationId) {
        selectedLocation = configuredLocation(locationId);

        assertThat(selectedLocation)
                .as("configured parking location %s", locationId)
                .isNotNull()
                .extracting(ParkingLocation::id)
                .isEqualTo(locationId);
    }

    @Given("parking location {long} has a capacity of {int}")
    public void parkingLocationHasCapacity(long locationId, int capacity) {
        selectedLocation = configuredLocation(locationId);
        assertThat(selectedLocation.maxCapacity())
                .as("configured capacity of location %s", locationId)
                .isEqualTo(capacity);
    }

    @Given("parking location {long} has {int} active stay(s)")
    public void parkingLocationHasActiveStays(long locationId, int count) {
        ParkingLocation location = configuredLocation(locationId);
        assertThat(count)
                .as("fixture stay count for location %s", locationId)
                .isBetween(0, location.maxCapacity());
        assertThat(activeStays.countByLocationId(locationId))
                .as("location %s is empty before scenario setup", locationId)
                .isZero();

        for (int spaceNumber = 1; spaceNumber <= count; spaceNumber++) {
            ParkingStay stay = activeStayAt(locationId, spaceNumber);
            assertThat(activeStays.tryAdd(stay))
                    .as("fixture session %s is accepted", stay.vehicleRegistration())
                    .isTrue();
        }

        expectedActiveStays.put(locationId, count);
    }

    @When("I request its parking availability")
    public void iRequestItsParkingAvailability() {
        selectedLocation = configuredLocation(parkingProperties.getDefaultLocationId());
        requestAvailability("/parking");
    }

    @When("I request parking availability for location {long}")
    public void iRequestParkingAvailabilityForLocation(long locationId) {
        selectedLocation = configuredLocation(locationId);
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
        assertThat(selectedLocation)
                .as("location selected by the scenario")
                .isNotNull();
        thereShouldBeAvailableSpaces(selectedLocation.maxCapacity());
    }

    @Then("the number of available spaces should match its maximum capacity minus the active stays")
    public void availableSpacesShouldMatchCapacityMinusActiveStays() {
        assertThat(selectedLocation)
                .as("location selected by the scenario")
                .isNotNull();
        assertThat(expectedActiveStays)
                .as("expected fixture counts by location")
                .containsKey(selectedLocation.id());

        int expectedAvailable = selectedLocation.maxCapacity()
                - expectedActiveStays.get(selectedLocation.id());
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

    private ParkingStay activeStayAt(long locationId, int spaceNumber) {
        return new ParkingStay(
                UUID.randomUUID(),
                locationId,
                spaceNumber,
                "L" + locationId + "CAR" + spaceNumber,
                VehicleType.SMALL,
                clock.instant(),
                null,
                null
        );
    }

    private ParkingLocation configuredLocation(long locationId) {
        return parkingProperties.getLocations().stream()
                .filter(location -> location.id() == locationId)
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "Expected configured parking location " + locationId));
    }

    private void requestAvailability(String path) {
        response = client.get()
                .uri(path)
                .retrieve()
                .toEntity(OccupancyResponse.class);

        assertThat(response.getStatusCode())
                .as("occupancy response status")
                .isEqualTo(HttpStatus.OK);
    }

    /**
     * Represents the occupancy JSON contract as seen by an external HTTP client.
     * Kept independent of application DTOs so a server DTO change does not
     * automatically change the response structure expected by these tests.
     */
    public record OccupancyResponse(
            long locationId,
            long availableSpaces,
            long occupiedSpaces,
            Instant timeChecked
    ) {
    }
}
