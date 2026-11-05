package com.cp.tds.parking.acceptance;

import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.Map;

/**
 * Holds shared test state for a single Cucumber scenario.
 */
public class ScenarioContext {

    private final Map<Long, Integer> expectedActiveStays = new HashMap<>();
    private ResponseEntity<?> parkingAttemptResponse;

    public void recordExpectedActiveStays(long locationId, int count) {
        expectedActiveStays.put(locationId, count);
    }

    public int expectedActiveStays(long locationId) {
        Integer count = expectedActiveStays.get(locationId);
        if (count == null) {
            throw new AssertionError("No fixture stay count recorded for location " + locationId);
        }
        return count;
    }

    public void recordParkingAttemptResponse(ResponseEntity<?> response) {
        parkingAttemptResponse = response;
    }

    public ResponseEntity<?> parkingAttemptResponse() {
        if (parkingAttemptResponse == null) {
            throw new AssertionError("No parking attempt has been made in this scenario");
        }
        return parkingAttemptResponse;
    }
}
