package com.cp.tds.parking.api.domain;

/**
 * Describes a configured parking location, its capacity, and its operating status.
 */
public record ParkingLocation(
        long id,
        String name,
        Integer maxCapacity,
        ParkingLocationStatus status) {}
