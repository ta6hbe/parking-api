package com.cp.tds.parking.api.persistence;

import com.cp.tds.parking.api.domain.ParkingStay;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * Defines counting and atomic registration-based insertion of active parking sessions.
 */
public interface ActiveParkingStayRepositoryI {

    long countByLocationId(long locationId);

    List<Integer> findOccupiedSpacesByLocationId(long locationId);

    OptionalInt findNextAvailableSpace(long locationId, int maxCapacity);

    Optional<ParkingStay> tryAdd(ParkingStay parkingStay);

    Optional<ParkingStay> findByVehicleRegistration(String vehicleRegistration);

    boolean remove(ParkingStay stay);

}
