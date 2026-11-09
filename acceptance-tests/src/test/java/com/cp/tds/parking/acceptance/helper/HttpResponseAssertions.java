package com.cp.tds.parking.acceptance.helper;

import com.cp.tds.parking.acceptance.dto.ProblemResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Checks the public Problem Details contract for any acceptance HTTP response.
 */
public final class HttpResponseAssertions {

    private HttpResponseAssertions() {
    }

    public static ProblemResponse assertProblem(ResponseEntity<?> response, int expectedStatus) {
        assertThat(response.getStatusCode().value())
                .as("HTTP error status").isEqualTo(expectedStatus);
        assertThat(response.getHeaders().getContentType())
                .as("Problem Details content type")
                .isNotNull()
                .satisfies(contentType -> assertThat(
                        contentType.isCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)).isTrue());
        assertThat(response.getBody())
                .as("Problem Details response body").isInstanceOf(ProblemResponse.class);
        ProblemResponse problem = (ProblemResponse) response.getBody();
        assertThat(problem.status()).as("problem status agrees with HTTP status")
                .isEqualTo(expectedStatus);
        assertThat(problem.title()).as("problem title")
                .isEqualTo(HttpStatus.valueOf(expectedStatus).getReasonPhrase());
        assertThat(problem.detail()).as("problem detail").isNotBlank();
        return problem;
    }
}
