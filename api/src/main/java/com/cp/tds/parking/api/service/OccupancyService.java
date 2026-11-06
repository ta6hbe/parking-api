package com.cp.tds.parking.api.service;

import com.cp.tds.parking.api.domain.ParkingLocation;
import com.cp.tds.parking.api.domain.ParkingLocationOccupancy;
import com.cp.tds.parking.api.persistence.ActiveParkingStayRepositoryI;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;

/**
 * Calculates occupancy for a configured location using active sessions and the shared clock.
 */
@Service
public class OccupancyService {

    private final LocationService locationService;
    private final ActiveParkingStayRepositoryI activeParkingStayRepository;
    private final Clock parkingClock;

    public OccupancyService(
            LocationService locationService,
            ActiveParkingStayRepositoryI activeParkingStayRepository,
            Clock parkingClock) {

        this.locationService = locationService;
        this.activeParkingStayRepository = activeParkingStayRepository;
        this.parkingClock = parkingClock;
    }

    public ParkingLocationOccupancy getOccupancy(long locationId) {
        return checkOccupancy(locationService.findLocationById(locationId));
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
