package com.cp.tds.parking.api.exception;

/**
 * Indicates that the requested vehicle no longer has the captured active stay.
 */
public class ActiveStayNotFoundException extends RuntimeException {

    public ActiveStayNotFoundException(String vehicleRegistration) {
        super("Active stay not found for vehicle registration: " + vehicleRegistration);
    }
}
