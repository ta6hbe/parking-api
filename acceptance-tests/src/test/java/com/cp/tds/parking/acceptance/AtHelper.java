package com.cp.tds.parking.acceptance;

import com.cp.tds.parking.acceptance.dto.OccupancyResponse;
import com.cp.tds.parking.acceptance.dto.PostVehicleDepartResponse;
import com.cp.tds.parking.acceptance.dto.PostVehicleParkResponse;
import com.cp.tds.parking.acceptance.dto.ProblemResponse;
import com.cp.tds.parking.acceptance.helper.AdjustableClock;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

import java.time.Instant;

/**
 * Sends acceptance HTTP requests and supplies scenario event timing.
 */
public class AtHelper {

    private final RestClient client;
    private final AdjustableClock clock;
    private final long defaultLocationId;

    public AtHelper(
            RestClient client,
            AdjustableClock clock,
            long defaultLocationId) {

        this.client = client;
        this.clock = clock;
        this.defaultLocationId = defaultLocationId;
    }

    public ResponseEntity<OccupancyResponse> getOccupancy(String path) {
        return client.get().uri(path).retrieve().toEntity(OccupancyResponse.class);
    }

    public ResponseEntity<ProblemResponse> getOccupancyProblem(long locationId) {
        return client.get().uri("/parking/" + locationId)
                .exchange((request, response) -> new ResponseEntity<>(
                        response.bodyTo(ProblemResponse.class),
                        response.getHeaders(), response.getStatusCode()));
    }

    /** Sends a parking request and reads either the success or Problem Details response. */
    public ResponseEntity<?> attemptToPark(String path, Object requestBody) {
        return attemptPost(path, requestBody, PostVehicleParkResponse.class);
    }

    public ResponseEntity<?> attemptToDepart(String path, Object requestBody) {
        return attemptPost(path, requestBody, PostVehicleDepartResponse.class);
    }

    public long getDefaultLocationId() {
        return defaultLocationId;
    }

    public void setRequestTime(Instant instant) {
        clock.setInstant(instant);
    }

    private <T> ResponseEntity<?> attemptPost(
            String path, Object requestBody, Class<T> successType) {
        return client.post()
                .uri(path)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .exchange((request, response) -> {
                    Object body = response.getStatusCode().is2xxSuccessful()
                            ? response.bodyTo(successType)
                            : response.bodyTo(ProblemResponse.class);
                    return new ResponseEntity<>(body, response.getHeaders(), response.getStatusCode());
                });
    }
}
