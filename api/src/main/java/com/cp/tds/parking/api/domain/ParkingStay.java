package com.cp.tds.parking.api.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Represents a parking session. Active sessions have no exit time or final charge;
 * completed sessions contain both.
 */
public record ParkingStay(
        UUID parkingSessionId,
        long locationId,
        int spaceNumber,
        String vehicleRegistration,
        VehicleType vehicleType,
        Instant timeIn,
        Instant timeOut,
        BigDecimal finalCharge
) {
}
