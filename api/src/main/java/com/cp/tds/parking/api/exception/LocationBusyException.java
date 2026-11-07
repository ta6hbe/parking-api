package com.cp.tds.parking.api.exception;

public class LocationBusyException extends RuntimeException {
    public LocationBusyException(long locationId, int maxRetries) {
        super("Unable to park at location ID: " + locationId + " after trying " + maxRetries + " times. Location too busy. Try again later");
    }
}
