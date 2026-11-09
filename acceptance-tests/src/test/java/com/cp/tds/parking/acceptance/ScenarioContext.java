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
    private ResponseEntity<?> vehicleParkingAttemptResponse;
    private ResponseEntity<?> vehicleDepartAttemptResponse;
    private ResponseEntity<?> attemptResponse;
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

    public void recordVehicleParkResponse(ResponseEntity<?> response) {
        vehicleParkingAttemptResponse = response;
        recordAttemptResponse(response);
    }

    public void recordVehicleDepartResponse(ResponseEntity<?> response) {
        vehicleDepartAttemptResponse = response;
        recordAttemptResponse(response);
    }

    public void recordAttemptResponse(ResponseEntity<?> response) {
        attemptResponse = response;
    }

    public ResponseEntity<?> vehicleParkingAttemptResponse() {
        if (vehicleParkingAttemptResponse == null) {
            throw new AssertionError("No vehicle parking attempt has been made in this scenario");
        }
        return vehicleParkingAttemptResponse;
    }

    public ResponseEntity<?> vehicleDepartAttemptResponse() {
        if (vehicleDepartAttemptResponse == null) {
            throw new AssertionError("No vehicle departure attempt has been made in this scenario");
        }
        return vehicleDepartAttemptResponse;
    }

    public ResponseEntity<?> attemptResponse() {
        if (attemptResponse == null) {
            throw new AssertionError("No parking attempt has been made in this scenario");
        }
        return attemptResponse;
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
