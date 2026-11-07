package com.cp.tds.parking.api.service;

import com.cp.tds.parking.api.config.ParkingProperties;
import com.cp.tds.parking.api.domain.ParkingLocation;
import com.cp.tds.parking.api.domain.ParkingStay;
import com.cp.tds.parking.api.domain.VehicleType;
import com.cp.tds.parking.api.exception.DuplicateRegistrationException;
import com.cp.tds.parking.api.exception.LocationBusyException;
import com.cp.tds.parking.api.exception.LocationFullException;
import com.cp.tds.parking.api.persistence.ActiveParkingStayRepositoryI;
import com.cp.tds.parking.api.persistence.LocationSpaceReservationRepositoryI;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * Coordinates vehicle parking using two repositories to prevent concurrent conflicts.
 *
 * <p>The reservation repository allows only one operation to reserve a location-space
 * pair at a time. After acquiring a reservation, the service rechecks active occupancy
 * and holds the reservation through insertion, then releases it in a finally block.
 * Operations on different spaces can proceed concurrently.
 *
 * <p>The active-stay repository uses atomic insertion by normalised registration to
 * prevent a vehicle from acquiring multiple active stays, even across locations.
 * Active stays determine the lowest available candidate and whether a location is full;
 * temporary reservations cause retries rather than counting as occupied spaces.
 *
 * <p>On success, the same stay is present in both repositories briefly: it becomes
 * active before its reservation is released, leaving no gap in space protection.
 * Departure operations must use the same reservation repository and recheck session
 * identity before changing an active stay.
 */
@Service
public class ParkingService {

    private final LocationService locationService;
    private final ActiveParkingStayRepositoryI activeParkingStayRepository;
    private final LocationSpaceReservationRepositoryI locationSpaceReservationRepository;

    private final Clock parkingClock;
    private final ParkingProperties parkingProperties;

    public ParkingService(
            LocationService locationService,
            ActiveParkingStayRepositoryI activeParkingStayRepository,
            LocationSpaceReservationRepositoryI locationSpaceReservationRepository,
            Clock parkingClock,
            ParkingProperties parkingProperties) {
        this.locationService = locationService;
        this.activeParkingStayRepository = activeParkingStayRepository;
        this.locationSpaceReservationRepository = locationSpaceReservationRepository;
        this.parkingClock = parkingClock;
        this.parkingProperties = parkingProperties;
    }

    public ParkingStay parkVehicle(long locationId, String vehicleRegistration, VehicleType vehicleType) {

        // Throws LocationNotFoundException if unknown, or LocationClosedException if arrivals are closed.
        ParkingLocation location = locationService.findOpenParkingLocationById(locationId);

        return safePark(location, vehicleRegistration, vehicleType);
    }

    /**
     * Selects a fresh candidate for each booking attempt until a stay is registered.
     * Allows one initial attempt plus the configured retries, then throws LocationBusyException.
     * Full-location and duplicate-registration errors stop booking immediately.
     */
    private ParkingStay safePark(ParkingLocation location, String vehicleRegistration, VehicleType vehicleType) {
        int maxRetries = parkingProperties.getMaxReservationRetryAttempts();
        int retries = 0;

        while (true) {
            ParkingStay proposedStay = generateReservation(vehicleRegistration, vehicleType, location);
            Optional<ParkingStay> parkedStay = tryPark(proposedStay);
            if (parkedStay.isPresent()) {
                return parkedStay.get();
            }
            if (retries >= maxRetries) {
                // Throws because contention exhausted the retry budget, not because the location is full.
                throw new LocationBusyException(location.id(), maxRetries);
            }
            retries++;
        }
    }

    /**
     * Reserves the candidate space, rechecks occupancy, and registers the same stay while reserved.
     * Returns empty if reservation or occupancy conflicts require another attempt.
     * Always releases an acquired reservation, including when registration throws.
     */
    private Optional<ParkingStay> tryPark(ParkingStay proposedStay) {
        // An existing reservation means another operation owns this space; retry allocation.
        if (locationSpaceReservationRepository.tryReserve(proposedStay).isPresent()) {
            return Optional.empty();
        }

        try {
            // Another booking may have taken the space between candidate selection and reservation.
            if (isTaken(proposedStay)) {
                return Optional.empty();
            }
            // Keep the reservation until the active stay has been inserted.
            registerStay(proposedStay);
            return Optional.of(proposedStay);
        } finally {
            // Release on success, an occupied-space retry, or an exception such as duplicate registration.
            locationSpaceReservationRepository.release(proposedStay);
        }
    }

    /**
     * Inserts the proposed stay into active storage while tryPark holds its space reservation.
     * Throws DuplicateRegistrationException if the registration already has an active stay.
     * On success, the stay is active before the temporary reservation is released.
     */
    private void registerStay(ParkingStay proposedStay) {
        // Atomic insertion rejects duplicate registrations across all locations without replacing the original stay.
        activeParkingStayRepository.tryAdd(proposedStay)
                .ifPresent(existingStay -> {
                    // A returned stay means this vehicle is already parked, even if at another location.
                    throw new DuplicateRegistrationException(
                            proposedStay.locationId(),
                            existingStay.locationId(),
                            existingStay.vehicleRegistration());
                });
    }

    /**
     * Builds a proposed stay for the lowest free space observed in active storage.
     * Does not acquire a reservation or save the stay; tryPark handles those steps.
     * Throws LocationFullException if no valid space is free in that observation.
     */
    private ParkingStay generateReservation(String vehicleRegistration, VehicleType vehicleType, ParkingLocation location) {

        // Throws LocationFullException only when active stays leave no valid space; reservations do not count as full.
        int nextAvailableSpaceNumber = activeParkingStayRepository.findNextAvailableSpace(
                location.id(), location.maxCapacity())
                .orElseThrow(() -> new LocationFullException(location.id()));

        return new ParkingStay(
                UUID.randomUUID(),
                location.id(),
                nextAvailableSpaceNumber,
                StringUtils.trimAllWhitespace(vehicleRegistration).toUpperCase(Locale.ROOT),
                vehicleType,
                Instant.now(parkingClock),
                null,
                null);
    }

    /**
     * Rechecks active occupancy after the candidate space has been reserved,
     * catching a stay inserted since candidate selection.
     */
    private boolean isTaken(ParkingStay parkingStay) {
        return activeParkingStayRepository.findOccupiedSpacesByLocationId(parkingStay.locationId())
                .contains(parkingStay.spaceNumber());
    }
}
