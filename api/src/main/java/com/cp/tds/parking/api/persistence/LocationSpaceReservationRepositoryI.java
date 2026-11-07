package com.cp.tds.parking.api.persistence;

import com.cp.tds.parking.api.domain.ParkingStay;

import java.util.Optional;

/**
 * Temporarily reserves spaces so parking and departure operations cannot
 * change the same space at the same time.
 */
public interface LocationSpaceReservationRepositoryI {

    /**
     * Finds a space's current reservation. This does not reserve the space.
     *
     * @return the current reservation, or empty if none exists
     */
    Optional<ParkingStay> findByLocationIdAndSpaceNo(long locationId, int spaceNo);

    /**
     * Atomically reserves the stay's space if no other operation has reserved it.
     *
     * @return empty on success, or the existing reservation if already reserved
     */
    Optional<ParkingStay> tryReserve(ParkingStay proposedStay);

    /**
     * Releases the space only if its reservation matches the supplied stay.
     * Note: Call this in a finally block after acquiring a reservation.
     *
     * @return true if released, or false if no matching reservation exists
     */
    boolean release(ParkingStay proposedStay);

}
