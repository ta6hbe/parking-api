package com.cp.tds.parking.api.service;

import com.cp.tds.parking.api.config.ParkingProperties;
import com.cp.tds.parking.api.domain.ParkingLocation;
import com.cp.tds.parking.api.domain.ParkingLocationOccupancy;
import com.cp.tds.parking.api.domain.ParkingStay;
import com.cp.tds.parking.api.exception.LocationNotFoundException;
import com.cp.tds.parking.api.persistence.ActiveParkingStayRepositoryI;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Calculates occupancy for a configured location using active sessions and the shared clock.
 */
@Service
public class OccupancyService {

    private final ParkingProperties parkingProperties;
    private final ActiveParkingStayRepositoryI activeParkingStayRepository;
    private final Clock parkingClock;

    public OccupancyService(
            ParkingProperties parkingProperties,
            ActiveParkingStayRepositoryI activeParkingStayRepository,
            Clock parkingClock) {

        this.parkingProperties = parkingProperties;
        this.activeParkingStayRepository = activeParkingStayRepository;
        this.parkingClock = parkingClock;
    }

    public ParkingLocationOccupancy getOccupancy(long locationId) {
        ParkingLocation location = findLocationById(locationId)
                .orElseThrow( () ->new LocationNotFoundException(locationId));

        return checkOccupancy(location);
    }

    private Optional<ParkingLocation> findLocationById(long locationId) {
        return parkingProperties.getLocations().stream()
                .filter(location -> location.id()  == locationId)
                .findFirst();
    }

    private ParkingLocationOccupancy checkOccupancy(ParkingLocation parkingLocation) {
        long activeStays = activeParkingStayRepository.countByLocationId(parkingLocation.id());
        return  new ParkingLocationOccupancy(
                parkingLocation.id(),
                parkingLocation.maxCapacity() - activeStays,
                activeStays,
                Instant.now(parkingClock));
    }
}
