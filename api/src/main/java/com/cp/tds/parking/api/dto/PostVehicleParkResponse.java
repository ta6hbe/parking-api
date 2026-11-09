package com.cp.tds.parking.api.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Describes a newly allocated parking session in the public API.
 */
public record PostVehicleParkResponse(
        UUID parkingSessionId,
        String vehicleReg,
        int spaceNumber,
        Instant timeIn) {
}
