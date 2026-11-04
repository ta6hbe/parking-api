package com.cp.tds.parking.api.persistence;

import com.cp.tds.parking.api.domain.ParkingStay;
import org.springframework.stereotype.Repository;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Stores active sessions in a concurrent map and rejects duplicate registration keys.
 */
@Repository
public class InMemoryActiveParkingStayRepository implements ActiveParkingStayRepositoryI {

    private final ConcurrentMap<String, ParkingStay> activeStays = new ConcurrentHashMap<>();

    @Override
    public long countByLocationId(long locationId) {
        return activeStays.values().stream()
                .filter(stay -> stay.locationId() == locationId)
                .count();
    }

    @Override
    public boolean tryAdd(ParkingStay stay) {
        return activeStays.putIfAbsent(
                stay.vehicleRegistration(), stay
        ) == null;
    }
}
