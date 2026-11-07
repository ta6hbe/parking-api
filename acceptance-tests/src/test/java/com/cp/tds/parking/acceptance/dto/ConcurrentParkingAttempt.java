package com.cp.tds.parking.acceptance.dto;

import org.springframework.http.ResponseEntity;

/** Associates a concurrent HTTP parking response with its requested location and vehicle. */
public record ConcurrentParkingAttempt(
        long locationId,
        PostParkingRequest request,
        ResponseEntity<?> response) {
}
