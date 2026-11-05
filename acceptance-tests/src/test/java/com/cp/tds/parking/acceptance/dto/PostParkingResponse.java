package com.cp.tds.parking.acceptance.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Represents the POST parking Response JSON contract as seen by an external HTTP client.
 * Kept independent of application DTOs so a server DTO change does not automatically
 * change the response structure expected by these tests.
 */
public record PostParkingResponse(
        UUID parkingSessionId,
        String vehicleReg,
        int spaceNumber,
        Instant timeIn) {}
