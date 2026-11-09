package com.cp.tds.parking.api.service;

import com.cp.tds.parking.api.domain.ParkingStay;
import com.cp.tds.parking.api.domain.VehicleType;
import com.cp.tds.parking.api.persistence.ActiveParkingStayRepositoryI;
import com.cp.tds.parking.api.persistence.LocationSpaceReservationRepositoryI;
import com.cp.tds.parking.api.persistence.ParkingStayHistoryRepositoryI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Checks departure results and coordinated storage changes through the public service.
 */
@ExtendWith(MockitoExtension.class)
class DepartServiceTest {

    private static final Instant TIME_IN =
            Instant.parse("2026-11-08T12:00:00Z");

    private static final Instant TIME_OUT =
            Instant.parse("2026-11-08T12:05:00Z");

    private static final String REGISTRATION = "AB12CDE";
    private static final BigDecimal CHARGE = new BigDecimal("1.50");

    @Mock
    private ActiveParkingStayRepositoryI activeStays;

    @Mock
    private LocationSpaceReservationRepositoryI reservations;

    @Mock
    private ParkingStayHistoryRepositoryI history;

    @Mock
    private BillService billService;

    private DepartService subject;

    @BeforeEach
    void setUp() {
        subject = new DepartService(
                activeStays,
                reservations,
                history,
                billService,
                Clock.fixed(TIME_OUT, ZoneOffset.UTC));
    }

    @Test
    void completesTheOriginalStayAndReleasesItsReservation() {
        ParkingStay active = activeStay();

        when(activeStays.findByVehicleRegistration(REGISTRATION))
                .thenReturn(Optional.of(active));
        when(reservations.tryReserve(active))
                .thenReturn(Optional.empty());
        when(billService.calculateCharge(active, TIME_OUT))
                .thenReturn(CHARGE);
        when(history.tryAdd(any(ParkingStay.class)))
                .thenReturn(Optional.empty());
        when(activeStays.remove(active))
                .thenReturn(true);

        ParkingStay completed = subject.departVehicle(" ab 12 cde ");

        assertThat(completed)
                .as("completed copy of the original active stay")
                .isNotSameAs(active)
                .extracting(
                        "parkingSessionId",
                        "vehicleRegistration",
                        "locationId",
                        "spaceNumber",
                        "vehicleType",
                        "timeIn",
                        "timeOut",
                        "finalCharge")
                .containsExactly(
                        active.parkingSessionId(),
                        REGISTRATION,
                        active.locationId(),
                        active.spaceNumber(),
                        active.vehicleType(),
                        TIME_IN,
                        TIME_OUT,
                        CHARGE);

        assertThat(active)
                .as("original active stay remains unchanged")
                .satisfies(stay -> {
                    assertThat(stay.timeOut()).isNull();
                    assertThat(stay.finalCharge()).isNull();
                });

        // Acquire the reservation lock before rechecking the stay and preparing its bill.
        // Store history before active removal commits departure, then release the lock once.
        var departureOrder = inOrder(activeStays, reservations, billService, history);
        departureOrder.verify(activeStays).findByVehicleRegistration(REGISTRATION);
        departureOrder.verify(reservations).tryReserve(active);
        departureOrder.verify(activeStays).findByVehicleRegistration(REGISTRATION);
        departureOrder.verify(billService).calculateCharge(active, TIME_OUT);
        departureOrder.verify(history).tryAdd(completed);
        departureOrder.verify(activeStays).remove(active);
        departureOrder.verify(reservations).release(active);

        // Ordered verification alone would not reject an additional premature release.
        verify(reservations, times(1)).release(active);
    }

    private ParkingStay activeStay() {
        return new ParkingStay(
                UUID.randomUUID(),
                1L,
                1,
                REGISTRATION,
                VehicleType.SMALL,
                TIME_IN,
                null,
                null);
    }
}
