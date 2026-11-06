package com.cp.tds.parking.api.exception;

public class DuplicateRegistrationException extends RuntimeException {

    public DuplicateRegistrationException(
            long requestedLocationId,
            long activeStaysLocationId,
            String vehicleRegistrationId) {

        super("Duplicate vehicle stay registration  attempt detected for requested location ID: %s and Vehicle ID: %s. Vehicle currently has active stay registration at Location ID: %s"
                .formatted(requestedLocationId, vehicleRegistrationId, activeStaysLocationId));

    }
}
