package com.cp.tds.parking.acceptance;

import com.cp.tds.parking.acceptance.dto.ProblemResponse;
import io.cucumber.java.en.Then;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies Problem Details returned by rejected parking attempts.
 */
public class ParkingVehicleErrorSteps {

    private final ScenarioContext context;

    public ParkingVehicleErrorSteps(ScenarioContext context) {
        this.context = context;
    }

    @Then("the parking rejection is a {int} problem")
    public void parkingRejectionIsAProblem(int expectedStatus) {
        var response = context.parkingAttemptResponse();
        assertThat(response.getStatusCode().value())
                .as("parking rejection HTTP status").isEqualTo(expectedStatus);
        MediaType contentType = response.getHeaders().getContentType();
        assertThat(contentType).as("problem content type").isNotNull();
        assertThat(contentType.isCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .as("response uses application/problem+json").isTrue();
        assertThat(response.getBody()).as("parking rejection body").isInstanceOf(ProblemResponse.class);
        ProblemResponse problem = (ProblemResponse) response.getBody();
        assertThat(problem.status()).as("problem status").isEqualTo(expectedStatus);
        assertThat(problem.title()).as("problem title").isNotBlank();
        assertThat(problem.detail()).as("problem detail").isNotBlank();
    }
}
