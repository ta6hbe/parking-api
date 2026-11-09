package com.cp.tds.parking.api.persistence;

import com.cp.tds.parking.api.domain.ParkingStay;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Stores completed stays in unbounded, volatile history keyed by session UUID.
 */
@Repository
public class InMemoryParkingStayHistoryRepository implements ParkingStayHistoryRepositoryI {

    private final ConcurrentMap<UUID, ParkingStay> history =
            new ConcurrentHashMap<>();

    @Override
    public Optional<ParkingStay> findByParkingSessionId(UUID parkingSessionId) {
        return Optional.ofNullable(history.get(parkingSessionId));
    }

    @Override
    public Optional<ParkingStay> tryAdd(ParkingStay completedStay) {
        return Optional.ofNullable(history.putIfAbsent(
                completedStay.parkingSessionId(), completedStay));
    }

    @Override
    public boolean remove(ParkingStay completedStay) {
        return history.remove(
                completedStay.parkingSessionId(), completedStay);
    }
}
