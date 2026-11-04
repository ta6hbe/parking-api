package com.cp.tds.parking.api.service;

import com.cp.tds.parking.api.config.ParkingProperties;
import com.cp.tds.parking.api.domain.ParkingLocation;
import com.cp.tds.parking.api.domain.ParkingLocationOccupancy;
import com.cp.tds.parking.api.domain.ParkingLocationStatus;

import com.cp.tds.parking.api.persistence.ActiveParkingStayRepositoryI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests occupancy calculations with mocked dependencies and a fixed clock.
 */
@ExtendWith(MockitoExtension.class)
class OccupancyServiceTest {

    static final ParkingLocation defaultParkingLocation = new ParkingLocation(
            1,
            "TDS Car Park 1",
            10,
            ParkingLocationStatus.ACTIVE);

    static final Instant EXPECTED_TIME = Instant.parse("2026-11-03T12:00:00.00Z");

    static ParkingLocationOccupancy emptuOccupancy = new ParkingLocationOccupancy(
            1,
            10,
            0,
            Instant.now(Clock.fixed(EXPECTED_TIME, ZoneOffset.UTC)));

    @Mock
    ParkingProperties parkingProperties;

    @Mock
    ActiveParkingStayRepositoryI activeParkingStayRepository;

    OccupancyService subject;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(EXPECTED_TIME, ZoneOffset.UTC);
        subject = new OccupancyService(parkingProperties, activeParkingStayRepository, clock);
    }

    @Test
    void getOccupancyReturnsCorrectFigures() {
        long locationId = 1L;

        // given parking location exists for locationId
        doReturn(List.of(defaultParkingLocation)).when(parkingProperties).getLocations();

        // and parking location is empty
        doReturn(0L).when(activeParkingStayRepository).countByLocationId(eq(locationId));

        // when service checks for occupancy of location id
        ParkingLocationOccupancy actualOccupancy = subject.getOccupancy(defaultParkingLocation.id());
        verify(parkingProperties, times(1)).getLocations();
        verify(activeParkingStayRepository, times(1)).countByLocationId(eq(locationId));

        // then
        assertThat(actualOccupancy)
                .isNotNull()
                .extracting(
                "locationId", "available", "occupied", "timeChecked")
                .containsExactly(
                        emptuOccupancy.locationId(),
                        emptuOccupancy.available(),
                        emptuOccupancy.occupied(),
                        emptuOccupancy.timeChecked());
    }
}
