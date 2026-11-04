package com.cp.tds.parking.api.exception;

/**
 * Indicates that the requested location ID is not present in the configured locations.
 */
public class LocationNotFoundException extends RuntimeException {

    public LocationNotFoundException(long locationId) {

        super("Parking location not found for Location ID: " + locationId);
    }
}
