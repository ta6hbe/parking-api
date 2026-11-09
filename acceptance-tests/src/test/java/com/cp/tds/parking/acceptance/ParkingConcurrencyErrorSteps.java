package com.cp.tds.parking.acceptance;

import com.cp.tds.parking.acceptance.dto.ConcurrentParkingAttempt;
import io.cucumber.java.en.Then;
import org.springframework.http.HttpStatus;

import java.util.List;

import static com.cp.tds.parking.acceptance.helper.HttpResponseAssertions.assertProblem;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies concurrent parking rejections using shared Problem Details assertions.
 */
public class ParkingConcurrencyErrorSteps {

    private final ScenarioContext context;

    public ParkingConcurrencyErrorSteps(ScenarioContext context) {
        this.context = context;
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

}
