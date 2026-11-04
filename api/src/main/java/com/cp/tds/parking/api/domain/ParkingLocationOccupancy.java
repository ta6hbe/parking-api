package com.cp.tds.parking.api.domain;

import java.time.Instant;

/**
 * Represents observed available and occupied space counts for a location at a given time.
 * Temporal data structure, always generated at the time of query. Never persisted.
 */
public record ParkingLocationOccupancy (
        long locationId,
        long available,
        long occupied,
        Instant timeChecked) {}
