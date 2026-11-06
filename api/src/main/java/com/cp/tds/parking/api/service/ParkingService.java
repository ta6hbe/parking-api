package com.cp.tds.parking.api.service;

import com.cp.tds.parking.api.domain.ParkingLocation;
import com.cp.tds.parking.api.domain.ParkingStay;
import com.cp.tds.parking.api.domain.VehicleType;
import com.cp.tds.parking.api.exception.DuplicateRegistrationException;
import com.cp.tds.parking.api.exception.LocationFullException;
import com.cp.tds.parking.api.persistence.ActiveParkingStayRepositoryI;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Service
public class ParkingService {

    private final LocationService locationService;
    private final ActiveParkingStayRepositoryI activeParkingStayRepository;
    private final Clock parkingClock;

    public ParkingService(
            LocationService locationService,
            ActiveParkingStayRepositoryI activeParkingStayRepository,
            Clock parkingClock) {
        this.locationService = locationService;
        this.activeParkingStayRepository = activeParkingStayRepository;
        this.parkingClock = parkingClock;
    }

    // needs to lock to protect from concurrency. Also transactional boundary here.
    public ParkingStay parkVehicle(
            long locationId,
            String vehicleRegistration,
            VehicleType vehicleType
    ) {
        ParkingLocation parkingLocation = locationService.findOpenParkingLocationById(locationId);
        // throws LocationNotFoundException if location with id locationId does not exist.
        // throws LocationFullException if location has no available parking spaces at time of check.
        // throws LocationClosedException if location is found but does not accept new stays.
        int nextAvailableSpaceNumber = activeParkingStayRepository.findNextAvailableSpace(
                parkingLocation.id(),
                parkingLocation.maxCapacity())
                .orElseThrow(()-> new LocationFullException(parkingLocation.id()));

        ParkingStay newParkingStay = new ParkingStay(
                UUID.randomUUID(),
                locationId,
                nextAvailableSpaceNumber,
                StringUtils.trimAllWhitespace(vehicleRegistration).toUpperCase(Locale.ROOT),
                vehicleType,
                Instant.now(parkingClock),
                null,
                null);

        // tryAdd returns previous existing found parking stay. So if present, we have found duplicate stay!
        // This will find active stays of existing vehicle registration in any location, irrespective of which
        // location is requrested in the current request.
        activeParkingStayRepository.tryAdd(newParkingStay)
                .ifPresent(duplicateStay -> handleDuplicateStayFound(duplicateStay, parkingLocation));

        return newParkingStay;
    }

    private void handleDuplicateStayFound(ParkingStay existingStay, ParkingLocation location) {
        throw new DuplicateRegistrationException(
                location.id(),
                existingStay.locationId(),
                existingStay.vehicleRegistration());
    }

}
