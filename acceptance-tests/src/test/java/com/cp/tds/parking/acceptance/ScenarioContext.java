package com.cp.tds.parking.acceptance;

import com.cp.tds.parking.acceptance.dto.ConcurrentParkingAttempt;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Holds shared test state for a single Cucumber scenario.
 */
public class ScenarioContext {

    private final Map<Long, Integer> expectedActiveStays = new HashMap<>();
    private ResponseEntity<?> parkingAttemptResponse;
    private List<ConcurrentParkingAttempt> concurrentParkingAttempts = List.of();

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

    public void recordConcurrentParkingAttempts(List<ConcurrentParkingAttempt> attempts) {
        concurrentParkingAttempts = List.copyOf(attempts);
    }

    public List<ConcurrentParkingAttempt> concurrentParkingAttempts() {
        if (concurrentParkingAttempts.isEmpty()) {
            throw new AssertionError("No concurrent parking attempts have been made in this scenario");
        }
        return concurrentParkingAttempts;
    }
}
