package com.cp.tds.parking.api.persistence;

import com.cp.tds.parking.api.domain.ParkingStay;

import java.util.Optional;
import java.util.UUID;

/**
 * Stores completed stays by session UUID and supports conditional departure cleanup.
 */
public interface ParkingStayHistoryRepositoryI {

    Optional<ParkingStay> findByParkingSessionId(UUID parkingSessionId);

    Optional<ParkingStay> tryAdd(ParkingStay completedStay);

    boolean remove(ParkingStay completedStay);
}
