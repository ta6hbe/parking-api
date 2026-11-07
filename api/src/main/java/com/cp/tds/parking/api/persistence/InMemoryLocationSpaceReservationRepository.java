package com.cp.tds.parking.api.persistence;

import com.cp.tds.parking.api.domain.ParkingStay;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stores temporary reservations by location and space in a concurrent map.
 * Atomic insertion prevents competing reservations; conditional removal
 * releases only the matching stay's reservation.
 */
@Repository
public class InMemoryLocationSpaceReservationRepository implements LocationSpaceReservationRepositoryI {

    private final ConcurrentHashMap<String, ParkingStay> parkingReservations;

    public InMemoryLocationSpaceReservationRepository() {
        this.parkingReservations = new ConcurrentHashMap<>();
    }

    @Override
    public Optional<ParkingStay> findByLocationIdAndSpaceNo(long locationId, int spaceNo) {
        return Optional.ofNullable(parkingReservations.get(locationId + "_" + spaceNo));
    }

    @Override
    public Optional<ParkingStay> tryReserve(ParkingStay newReservation) {
        return Optional.ofNullable(parkingReservations.putIfAbsent(
                newReservation.locationId() + "_" + newReservation.spaceNumber(),
                newReservation));
    }

    @Override
    public boolean release(ParkingStay proposedStay) {
        return parkingReservations.remove(proposedStay.locationId() + "_" + proposedStay.spaceNumber(), proposedStay);
    }
}
