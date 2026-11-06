package com.cp.tds.parking.api.service;

import com.cp.tds.parking.api.TestFixtures;
import com.cp.tds.parking.api.config.ParkingProperties;
import com.cp.tds.parking.api.domain.ParkingLocationOccupancy;
import com.cp.tds.parking.api.persistence.InMemoryActiveParkingStayRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Checks location-specific occupancy using a real in-memory repository and sample sessions.
 */
class OccupancyServiceWithRepositoryTest {

    private static final Instant EXPECTED_TIME =
            Instant.parse("2026-11-03T12:00:00Z");

    private OccupancyService subject;

    @BeforeEach
    void setUp() {
        var properties = new ParkingProperties();
        properties.setDefaultLocationId(TestFixtures.PARKING_LOCATION_1.id());
        properties.setLocations(TestFixtures.LOCATIONS_LIST);

        var repository = new InMemoryActiveParkingStayRepository();
        assertThat(repository.tryAdd(TestFixtures.ACTIVE_STAY_L1_1)).isEmpty();
        assertThat(repository.tryAdd(TestFixtures.ACTIVE_STAY_L1_2)).isEmpty();
        assertThat(repository.tryAdd(TestFixtures.ACTIVE_STAY_L2_1)).isEmpty();

        subject = new OccupancyService(
                new LocationService(properties),
                repository,
                Clock.fixed(EXPECTED_TIME, ZoneOffset.UTC)
        );
    }

    @Test
    void returnsOccupancyForEachLocationIndependently() {
        var first = subject.getOccupancy(TestFixtures.PARKING_LOCATION_1.id());
        var second = subject.getOccupancy(TestFixtures.PARKING_LOCATION_2.id());

        assertThat(first)
                .as("occupancy for location 1 with two active stays")
                .isEqualTo(new ParkingLocationOccupancy(
                        1L, 8L, 2L, EXPECTED_TIME));

        assertThat(second)
                .as("occupancy for location 2 with one active stay")
                .isEqualTo(new ParkingLocationOccupancy(
                        2L, 19L, 1L, EXPECTED_TIME));
    }
}
