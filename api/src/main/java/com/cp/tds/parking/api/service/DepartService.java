package com.cp.tds.parking.api.service;

import com.cp.tds.parking.api.domain.ParkingStay;
import com.cp.tds.parking.api.exception.ActiveStayNotFoundException;
import com.cp.tds.parking.api.exception.DepartureConflictException;
import com.cp.tds.parking.api.persistence.ActiveParkingStayRepositoryI;
import com.cp.tds.parking.api.persistence.LocationSpaceReservationRepositoryI;
import com.cp.tds.parking.api.persistence.ParkingStayHistoryRepositoryI;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.Locale;

/**
 * Coordinates billing and departure while holding the active stay's space reservation.
 * This is a thread safe concurrent safe departure service.
 */
@Service
public class DepartService {

    private final ActiveParkingStayRepositoryI activeStays;
    private final LocationSpaceReservationRepositoryI reservations;
    private final ParkingStayHistoryRepositoryI history;
    private final BillService billService;
    private final Clock parkingClock;

    public DepartService(
            ActiveParkingStayRepositoryI activeStays,
            LocationSpaceReservationRepositoryI reservations,
            ParkingStayHistoryRepositoryI history,
            BillService billService,
            Clock parkingClock) {

        this.activeStays = activeStays;
        this.reservations = reservations;
        this.history = history;
        this.billService = billService;
        this.parkingClock = parkingClock;
    }

    /**
     * Completes the active stay while holding its location-space reservation lock.
     *
     * @throws ActiveStayNotFoundException if the captured stay is absent or replaced
     * @throws DepartureConflictException if the reservation lock cannot be acquired
     */
    public ParkingStay departVehicle(String vehicleRegistration) {
        String registration = StringUtils.trimAllWhitespace(vehicleRegistration)
                .toUpperCase(Locale.ROOT);

        ParkingStay active = findActiveStay(registration);

        // An existing reservation belongs to another operation; reject without retrying.
        if (reservations.tryReserve(active).isPresent()) {
            throw new DepartureConflictException();
        }

        try {
            confirmSameActiveStay(active);
            ParkingStay completed = prepareCompletedStay(active);
            storeHistoryAndRemoveActive(active, completed);
            return completed;
        } finally {
            // Release only the reservation lock acquired by this departure.
            reservations.release(active);
        }
    }

    private ParkingStay findActiveStay(String registration) {
        return activeStays.findByVehicleRegistration(registration)
                .orElseThrow(() -> new ActiveStayNotFoundException(registration));
    }

    private void confirmSameActiveStay(ParkingStay original) {
        ParkingStay current = findActiveStay(original.vehicleRegistration());

        if (!current.parkingSessionId().equals(original.parkingSessionId())) {
            throw new ActiveStayNotFoundException(original.vehicleRegistration());
        }
    }

    private ParkingStay prepareCompletedStay(ParkingStay active) {
        Instant timeOut = parkingClock.instant();
        BigDecimal charge = billService.calculateCharge(active, timeOut);

        return new ParkingStay(
                active.parkingSessionId(),
                active.locationId(),
                active.spaceNumber(),
                active.vehicleRegistration(),
                active.vehicleType(),
                active.timeIn(),
                timeOut,
                charge);
    }

    private void storeHistoryAndRemoveActive(
            ParkingStay active, ParkingStay completed) {

        if (history.tryAdd(completed).isPresent()) {
            // This departure owns no insertion, so it must not remove existing history.
            throw new IllegalStateException(
                    "History already exists for parking session: "
                            + completed.parkingSessionId());
        }

        try {
            if (!activeStays.remove(active)) {
                throw new IllegalStateException(
                        "Could not remove the protected active parking session: "
                                + active.parkingSessionId());
            }
        } catch (RuntimeException removalFailure) {
            // The repository must report removal failure only before changing storage.
            undoHistoryInsertion(completed, removalFailure);
            throw removalFailure;
        }

        // Successful active removal commits departure. No rollback follows this point.
    }

    private void undoHistoryInsertion(
            ParkingStay completed, RuntimeException removalFailure) {

        try {
            if (!history.remove(completed)) {
                removalFailure.addSuppressed(new IllegalStateException(
                        "Could not undo history insertion for parking session: "
                                + completed.parkingSessionId()));
            }
        } catch (RuntimeException cleanupFailure) {
            // Preserve the original removal failure and report cleanup failure alongside it.
            removalFailure.addSuppressed(cleanupFailure);
        }
    }
}
