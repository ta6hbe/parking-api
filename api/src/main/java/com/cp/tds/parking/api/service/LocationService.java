package com.cp.tds.parking.api.service;

import com.cp.tds.parking.api.config.ParkingProperties;
import com.cp.tds.parking.api.domain.ParkingLocation;
import com.cp.tds.parking.api.exception.LocationNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Resolves configured parking locations and rejects unknown location IDs.
 */
@Service
public class LocationService {

    private final ParkingProperties parkingProperties;

    public LocationService(ParkingProperties parkingProperties) {
        this.parkingProperties = parkingProperties;
    }

    public ParkingLocation findLocationById(long locationId) {
        return parkingProperties.getLocations().stream()
                .filter(location -> location.id() == locationId)
                .findFirst()
                .orElseThrow(() -> new LocationNotFoundException(locationId));
    }
}
