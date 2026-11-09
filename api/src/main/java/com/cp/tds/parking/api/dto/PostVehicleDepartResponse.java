package com.cp.tds.parking.api.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Describes a completed parking session and its final bill in the public API.
 */
public record PostVehicleDepartResponse(
        UUID parkingSessionId,
        String vehicleReg,
        Double vehicleCharge,
        Instant timeIn,
        Instant timeOut
) {
}
