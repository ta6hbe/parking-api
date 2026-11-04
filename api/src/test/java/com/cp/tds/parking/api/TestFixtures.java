package com.cp.tds.parking.api;

import com.cp.tds.parking.api.domain.ParkingLocation;
import com.cp.tds.parking.api.domain.ParkingLocationStatus;
import com.cp.tds.parking.api.domain.ParkingStay;
import com.cp.tds.parking.api.domain.VehicleType;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Provides sample locations and parking sessions for tests.
 */
public class TestFixtures {

    public static final ParkingLocation PARKING_LOCATION_1 =
            new ParkingLocation(1, "TDS Car Park 1", 10, ParkingLocationStatus.ACTIVE);
    public static final ParkingLocation PARKING_LOCATION_2 =
            new ParkingLocation(2, "TDS Car Park 2", 20, ParkingLocationStatus.ACTIVE);

    public static final ParkingStay ACTIVE_STAY_L1_1 =
            new ParkingStay(UUID.randomUUID(),
                    PARKING_LOCATION_1.id(),
                    1,
                    "NC123ABC",
                    VehicleType.SMALL,
                    Instant.now().minus(2, ChronoUnit.HOURS),
                    null,
                    null);

    public static final ParkingStay ACTIVE_STAY_L1_2 =
            new ParkingStay(UUID.randomUUID(),
                    PARKING_LOCATION_1.id(),
                    2,
                    "NC456DEF",
                    VehicleType.LARGE,
                    Instant.now().minus(3, ChronoUnit.HOURS),
                    null,
                    null);

    public static final ParkingStay ACTIVE_STAY_L2_1 =
            new ParkingStay(UUID.randomUUID(),
                    PARKING_LOCATION_2.id(),
                    1,
                    "AB432CDE",
                    VehicleType.MEDIUM,
                    Instant.now().minus(1, ChronoUnit.HOURS),
                    null,
                    null);

    public static final ParkingStay ACTIVE_STAY_L2_2 =
            new ParkingStay(UUID.randomUUID(),
                    PARKING_LOCATION_2.id(),
                    2,
                    "CDD432CDE",
                    VehicleType.SMALL,
                    Instant.now().minus(1, ChronoUnit.HOURS),
                    null,
                    null);

    static List<ParkingStay> parkingStays = List.of(ACTIVE_STAY_L1_1, ACTIVE_STAY_L1_2, ACTIVE_STAY_L2_1,  ACTIVE_STAY_L2_2);

    public static List<ParkingLocation> LOCATIONS_LIST = List.of(PARKING_LOCATION_1, PARKING_LOCATION_2);

    public static final ConcurrentHashMap<String, ParkingStay> activeStays = parkingStays.stream()
            .collect(Collectors.toMap(
                    ParkingStay::vehicleRegistration,
                    stay -> stay,
                    (existing, replacement) -> existing, // Merge rule for duplicate registration keys
                    ConcurrentHashMap::new
            ));
}
