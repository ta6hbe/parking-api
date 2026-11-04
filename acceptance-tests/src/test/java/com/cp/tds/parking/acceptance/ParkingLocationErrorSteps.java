package com.cp.tds.parking.acceptance;

import com.cp.tds.parking.api.config.ParkingProperties;
import com.cp.tds.parking.api.domain.ParkingLocation;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the HTTP Problem Details contract for unknown parking locations.
 */
public class ParkingLocationErrorSteps {

    private final ParkingProperties parkingProperties;
    private final RestClient client;

    private ResponseEntity<ProblemResponse> response;
    private long requestedLocationId;

    public ParkingLocationErrorSteps(Environment environment, ParkingProperties parkingProperties) {
        this.parkingProperties = parkingProperties;
        int port = environment.getRequiredProperty("server.port", Integer.class);
        this.client = RestClient.create("http://localhost:" + port);
    }

    @Given("parking location {long} is not configured")
    public void parkingLocationIsNotConfigured(long locationId) {
        assertThat(parkingProperties.getLocations())
                .as("configured location IDs")
                .extracting(ParkingLocation::id)
                .doesNotContain(locationId);
    }

    @When("I request parking availability for an unknown location {long}")
    public void iRequestAvailabilityForAnUnknownLocation(long locationId) {
        requestedLocationId = locationId;
        response = client.get()
                .uri("/parking/" + locationId)
                .retrieve()
                .onStatus(status -> status.value() == 404, (request, clientResponse) -> {
                    // Preserve the expected 404 response for contract assertions.
                })
                .toEntity(ProblemResponse.class);
    }

    @Then("the response should be a 404 problem")
    public void responseShouldBeANotFoundProblem() {
        assertThat(response.getStatusCode())
                .as("unknown-location response status")
                .isEqualTo(HttpStatus.NOT_FOUND);

        MediaType contentType = response.getHeaders().getContentType();
        assertThat(contentType)
                .as("problem response content type")
                .isNotNull();
        assertThat(contentType.isCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .as("response uses application/problem+json")
                .isTrue();

        assertThat(response.getBody())
                .as("problem response body")
                .isNotNull();
        assertThat(response.getBody().status())
                .as("problem status agrees with HTTP status")
                .isEqualTo(404);
        assertThat(response.getBody().title())
                .as("problem title")
                .isNotBlank();
        assertThat(response.getBody().detail())
                .as("problem detail identifies the missing location")
                .contains(Long.toString(requestedLocationId));
    }

    /**
     * Represents expected problem fields independently of the server error implementation.
     */
    public record ProblemResponse(int status, String title, String detail) {
    }
}
