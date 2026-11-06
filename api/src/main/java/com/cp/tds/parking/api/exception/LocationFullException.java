package com.cp.tds.parking.api.exception;

/**
 * Indicates that a parking location has no available spaces.
 */
public class LocationFullException extends RuntimeException {

    public LocationFullException(long locationId) {
        super("Parking location " + locationId + " is full");
    }
}
