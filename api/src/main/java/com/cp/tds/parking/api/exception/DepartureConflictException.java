package com.cp.tds.parking.api.exception;

/**
 * Indicates that departure could not acquire the active stay's
 * location-space reservation lock because another operation held it.
 */
public class DepartureConflictException extends RuntimeException {

    public DepartureConflictException() {
        super("The parking session cannot be completed at this time because of a concurrent operation. Please try again.");
    }
}
