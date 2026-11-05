package com.cp.tds.parking.acceptance;

import com.cp.tds.parking.acceptance.dto.ProblemResponse;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the HTTP Problem Details contract for unknown parking locations.
 */
public class ParkingLocationErrorSteps {

    private final AtHelper helper;
    private ResponseEntity<ProblemResponse> response;
    private long requestedLocationId;

    public ParkingLocationErrorSteps(AtHelper helper) {
        this.helper = helper;
    }

    @When("I request parking availability for an unknown location {long}")
    public void iRequestAvailabilityForAnUnknownLocation(long locationId) {
        requestedLocationId = locationId;
        response = helper.getClient().get()
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
}
