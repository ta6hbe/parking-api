package com.cp.tds.parking.acceptance;

import com.cp.tds.parking.api.config.ParkingProperties;
import com.cp.tds.parking.api.domain.ParkingLocation;
import com.cp.tds.parking.api.domain.ParkingStay;
import com.cp.tds.parking.api.domain.VehicleType;
import com.cp.tds.parking.api.persistence.ActiveParkingStayRepositoryI;
import org.springframework.web.client.RestClient;

import java.time.Clock;
import java.util.Locale;
import java.util.UUID;

/**
 * Provides acceptance dependencies, location lookup, and fixture construction.
 */
public class AtHelper {

    private final ParkingProperties parkingProperties;
    private final RestClient client;
    private final ActiveParkingStayRepositoryI activeStays;
    private final Clock clock;

    public AtHelper(
            ParkingProperties parkingProperties,
            RestClient client,
            ActiveParkingStayRepositoryI activeStays,
            Clock clock) {

        this.parkingProperties = parkingProperties;
        this.client = client;
        this.activeStays = activeStays;
        this.clock = clock;
    }

    public ParkingLocation configuredLocation(long locationId) {
        return parkingProperties.getLocations().stream()
                .filter(location -> location.id() == locationId)
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "Expected configured parking location " + locationId));
    }

    public ParkingStay activeStayAt(long locationId, int spaceNumber) {
        return new ParkingStay(
                UUID.randomUUID(),
                locationId,
                spaceNumber,
                "L" + locationId + String.format(Locale.ROOT, "%02d", spaceNumber) + "CAR",
                VehicleType.SMALL,
                clock.instant(),
                null,
                null
        );
    }

    public ParkingProperties getParkingProperties() {
        return parkingProperties;
    }

    public RestClient getClient() {
        return client;
    }

    public ActiveParkingStayRepositoryI getActiveStays() {
        return activeStays;
    }

    public Clock getClock() {
        return clock;
    }
}
