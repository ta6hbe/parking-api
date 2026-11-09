package com.cp.tds.parking.acceptance;

import io.cucumber.java.en.Then;

import static com.cp.tds.parking.acceptance.helper.HttpResponseAssertions.assertProblem;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies status and rejection messages from the latest HTTP response.
 */
public class HttpResponseSteps {

    private final ScenarioContext context;

    public HttpResponseSteps(ScenarioContext context) {
        this.context = context;
    }

    @Then("I get a {int} http response status")
    public void responseHasStatus(int expectedStatus) {
        assertThat(context.attemptResponse().getStatusCode().value())
                .as("HTTP response status").isEqualTo(expectedStatus);
    }

    @Then("the response is a {int} problem containing {string}")
    public void responseIsAProblemContaining(int expectedStatus, String expectedDetail) {
        assertThat(assertProblem(context.attemptResponse(), expectedStatus).detail())
                .as("problem detail explains the rejection")
                .containsIgnoringCase(expectedDetail);
    }
}
