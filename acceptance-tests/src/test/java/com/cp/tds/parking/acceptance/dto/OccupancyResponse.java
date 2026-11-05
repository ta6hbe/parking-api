package com.cp.tds.parking.acceptance.dto;

import java.time.Instant;

/**
 * Represents the occupancy JSON contract as seen by an external HTTP client.
 * Kept independent of application DTOs so a server DTO change does not
 * automatically change the response structure expected by these tests.
 */
public record OccupancyResponse(
        long locationId,
        long availableSpaces,
        long occupiedSpaces,
        Instant timeChecked
) { }
