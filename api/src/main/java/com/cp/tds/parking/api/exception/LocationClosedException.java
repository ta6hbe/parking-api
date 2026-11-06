package com.cp.tds.parking.api.exception;

/**
 * Indicates that a parking location is closed to new arrivals.
 */
public class LocationClosedException extends RuntimeException {

    public LocationClosedException(long locationId) {
        super("Parking location " + locationId + " is closed");
    }
}
