package com.cp.tds.parking.api.dto;

import java.time.Instant;

/**
 * Response object mapping directly domain model ParkingLocationOccupancy.
 * Shields changes in domain model from leaking into the API responses.
 * Loosely couples domain and presentation layers.
 */
public record GetParkingResponse(
        long locationId,
        long availableSpaces,
        long occupiedSpaces,
        Instant timeChecked
) {
}
