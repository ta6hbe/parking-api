package com.cp.tds.parking.acceptance;

import com.cp.tds.parking.acceptance.dto.ConcurrentParkingAttempt;
import com.cp.tds.parking.acceptance.dto.ProblemResponse;
import io.cucumber.java.en.Then;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.List;

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
        assertProblem(context.parkingAttemptResponse(), expectedStatus);
    }

    @Then("{int} concurrent parking attempts are rejected with a {int} problem containing {string}")
    public void concurrentParkingAttemptsAreRejected(int count, int status, String reason) {
        List<ConcurrentParkingAttempt> rejections = unsuccessfulAttempts();
        assertThat(rejections).as("rejected concurrent requests").hasSize(count);
        assertThat(rejections).allSatisfy(attempt ->
                assertThat(assertProblem(attempt.response(), status).detail()).containsIgnoringCase(reason));
    }

    @Then("all unsuccessful concurrent parking attempts are {int} problems")
    public void unsuccessfulConcurrentAttemptsAreProblems(int status) {
        List<ConcurrentParkingAttempt> rejections = unsuccessfulAttempts();
        assertThat(rejections).as("requests rejected while competing for limited capacity").isNotEmpty();
        assertThat(rejections).allSatisfy(attempt -> assertProblem(attempt.response(), status));
    }

    @Then("concurrent rejections report a full or busy location")
    public void concurrentRejectionsReportFullOrBusy() {
        assertThat(unsuccessfulAttempts()).isNotEmpty().allSatisfy(attempt ->
                assertThat(assertProblem(attempt.response(), HttpStatus.CONFLICT.value()).detail())
                        .containsAnyOf("is full", "too busy"));
    }

    private List<ConcurrentParkingAttempt> unsuccessfulAttempts() {
        return context.concurrentParkingAttempts().stream()
                .filter(attempt -> !attempt.response().getStatusCode().equals(HttpStatus.CREATED)).toList();
    }

    private ProblemResponse assertProblem(ResponseEntity<?> response, int expectedStatus) {
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
        return problem;
    }
}
