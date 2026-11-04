package com.cp.tds.parking.api.persistence;

import com.cp.tds.parking.api.domain.ParkingStay;

/**
 * Defines counting and atomic registration-based insertion of active parking sessions.
 */
public interface ActiveParkingStayRepositoryI {

    long countByLocationId(long locationId);

    boolean tryAdd(ParkingStay parkingStay);

}
