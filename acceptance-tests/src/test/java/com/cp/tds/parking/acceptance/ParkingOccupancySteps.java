package com.cp.tds.parking.acceptance;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises the occupancy HTTP endpoint and checks its response in Cucumber scenarios.
 */
public class ParkingOccupancySteps {

    private final Environment environment;
    private final RestClient client;

    private ResponseEntity<OccupancyResponse> response;

    public ParkingOccupancySteps(Environment environment) {
        this.environment = environment;

        int port = environment.getRequiredProperty("server.port", Integer.class);
        this.client = RestClient.create("http://localhost:" + port);
    }

    @Given("the default parking location has a capacity of {int}")
    public void theDefaultParkingLocationHasCapacity(int capacity) {
        assertThat(environment.getRequiredProperty(
                "parking.locations[0].max-capacity", Integer.class))
                .as("configured parking capacity")
                .isEqualTo(capacity);
    }

    @When("I request its parking availability")
    public void iRequestItsParkingAvailability() {
        response = client.get()
                .uri("/parking")
                .retrieve()
                .toEntity(OccupancyResponse.class);

        assertThat(response.getStatusCode())
                .as("occupancy response status")
                .isEqualTo(HttpStatus.OK);
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

    /**
     * Models the occupancy response independently of application DTOs.
     */
    public record OccupancyResponse(
            int availableSpaces,
            int occupiedSpaces
    ) {
    }
}
