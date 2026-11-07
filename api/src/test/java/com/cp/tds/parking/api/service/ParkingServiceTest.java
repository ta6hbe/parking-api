package com.cp.tds.parking.api.service;

import com.cp.tds.parking.api.config.ParkingProperties;
import com.cp.tds.parking.api.domain.ParkingLocation;
import com.cp.tds.parking.api.domain.ParkingLocationStatus;
import com.cp.tds.parking.api.domain.ParkingStay;
import com.cp.tds.parking.api.domain.VehicleType;
import com.cp.tds.parking.api.exception.DuplicateRegistrationException;
import com.cp.tds.parking.api.exception.LocationBusyException;
import com.cp.tds.parking.api.exception.LocationClosedException;
import com.cp.tds.parking.api.exception.LocationFullException;
import com.cp.tds.parking.api.exception.LocationNotFoundException;
import com.cp.tds.parking.api.persistence.ActiveParkingStayRepositoryI;
import com.cp.tds.parking.api.persistence.LocationSpaceReservationRepositoryI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/** Checks booking results, exceptions, and reservation cleanup through the public parking service. */
@ExtendWith(MockitoExtension.class)
class ParkingServiceTest {

    private static final ParkingLocation LOCATION =
            new ParkingLocation(1L, "Location 1", 10, ParkingLocationStatus.ACTIVE);
    private static final Instant EXPECTED_TIME = Instant.parse("2026-11-07T12:00:00Z");
    private static final String REGISTRATION = "AB12CDE";

    @Mock
    private LocationService locationService;
    @Mock
    private ActiveParkingStayRepositoryI activeStays;
    @Mock
    private LocationSpaceReservationRepositoryI reservations;
    @Captor
    private ArgumentCaptor<ParkingStay> stayCaptor;

    private ParkingProperties properties;
    private ParkingService subject;

    @BeforeEach
    void setUp() {
        properties = new ParkingProperties();
        properties.setMaxReservationRetryAttempts(2);
        subject = new ParkingService(locationService, activeStays, reservations,
                Clock.fixed(EXPECTED_TIME, ZoneOffset.UTC), properties);
    }

    @Test
    void returnsTheNormalisedActiveStayAndReleasesItsReservation() {
        givenAvailableSpace(3);
        givenReservationAcquired();
        givenSpaceUnoccupied();
        givenRegistrationAccepted();

        ParkingStay result = subject.parkVehicle(LOCATION.id(), " ab12 cde ", VehicleType.SMALL);

        assertThat(result).isNotNull().satisfies(stay -> {
            assertThat(stay.parkingSessionId()).isNotNull();
            assertThat(stay.locationId()).isEqualTo(LOCATION.id());
            assertThat(stay.spaceNumber()).isEqualTo(3);
            assertThat(stay.vehicleRegistration()).isEqualTo(REGISTRATION);
            assertThat(stay.vehicleType()).isEqualTo(VehicleType.SMALL);
            assertThat(stay.timeIn()).isEqualTo(EXPECTED_TIME);
            assertThat(stay.timeOut()).isNull();
            assertThat(stay.finalCharge()).isNull();
        });
        verify(activeStays).tryAdd(same(result));
        verify(reservations).release(same(result));
    }

    @Test
    void rejectsAnUnknownLocationBeforeAttemptingABooking() {
        var failure = new LocationNotFoundException(999L);
        doThrow(failure).when(locationService).findOpenParkingLocationById(999L);

        assertThatThrownBy(() -> subject.parkVehicle(999L, REGISTRATION, VehicleType.SMALL))
                .isInstanceOf(LocationNotFoundException.class).isSameAs(failure);
        verifyNoInteractions(activeStays, reservations);
    }

    @Test
    void rejectsAClosedLocationBeforeAttemptingABooking() {
        var failure = new LocationClosedException(LOCATION.id());
        doThrow(failure).when(locationService).findOpenParkingLocationById(LOCATION.id());

        assertThatThrownBy(this::park)
                .isInstanceOf(LocationClosedException.class).isSameAs(failure);
        verifyNoInteractions(activeStays, reservations);
    }

    @Test
    void rejectsAFullLocationWithoutTryingToReserveASpace() {
        givenOpenLocation();
        doReturn(OptionalInt.empty()).when(activeStays)
                .findNextAvailableSpace(LOCATION.id(), LOCATION.maxCapacity());

        assertThatThrownBy(this::park).isInstanceOf(LocationFullException.class);
        verifyNoInteractions(reservations);
        verify(activeStays, never()).tryAdd(any(ParkingStay.class));
    }

    @Test
    void reportsBothLocationsForADuplicateAndReleasesTheReservation() {
        givenAvailableSpace(1);
        givenReservationAcquired();
        givenSpaceUnoccupied();
        ParkingStay existingStay = existingStay(2L, 4);
        doReturn(Optional.of(existingStay)).when(activeStays).tryAdd(any(ParkingStay.class));

        assertThatThrownBy(this::park)
                .isInstanceOf(DuplicateRegistrationException.class)
                .hasMessageContaining("requested location ID: 1")
                .hasMessageContaining("Location ID: 2")
                .hasMessageContaining(REGISTRATION);
        assertAcquiredReservationReleased();
    }

    // Retry budgets: 0 permits only the initial attempt; 2 permits three attempts in total.
    @ParameterizedTest
    @ValueSource(ints = {0, 2})
    void rejectsABusyLocationAfterTheInitialAttemptAndConfiguredRetries(int maxRetries) {
        properties.setMaxReservationRetryAttempts(maxRetries);
        givenAvailableSpace(1);
        doReturn(Optional.of(existingStay(LOCATION.id(), 1)))
                .when(reservations).tryReserve(any(ParkingStay.class));

        assertThatThrownBy(this::park).isInstanceOf(LocationBusyException.class);
        verify(reservations, times(maxRetries + 1)).tryReserve(any(ParkingStay.class));
        verify(reservations, never()).release(any(ParkingStay.class));
        verify(activeStays, never()).tryAdd(any(ParkingStay.class));
    }

    @Test
    void booksSuccessfullyAfterATemporaryReservationConflict() {
        givenAvailableSpace(1);
        doReturn(Optional.of(existingStay(LOCATION.id(), 1)), Optional.empty())
                .when(reservations).tryReserve(any(ParkingStay.class));
        givenSpaceUnoccupied();
        givenRegistrationAccepted();

        ParkingStay stay = park();

        assertThat(stay.spaceNumber()).isEqualTo(1);
        verify(reservations).release(same(stay));
    }

    @Test
    void releasesAnOccupiedCandidateAndBooksTheNextAvailableSpace() {
        givenOpenLocation();
        doReturn(OptionalInt.of(1), OptionalInt.of(2)).when(activeStays)
                .findNextAvailableSpace(LOCATION.id(), LOCATION.maxCapacity());
        givenReservationAcquired();
        doReturn(List.of(1)).when(activeStays).findOccupiedSpacesByLocationId(LOCATION.id());
        givenRegistrationAccepted();

        ParkingStay stay = park();

        assertThat(stay.spaceNumber()).isEqualTo(2);
        verify(reservations, times(2)).release(stayCaptor.capture());
        assertThat(stayCaptor.getAllValues()).extracting(ParkingStay::spaceNumber).containsExactly(1, 2);
        verify(activeStays).tryAdd(same(stay));
    }

    @Test
    void releasesTheReservationWhenTheOccupancyRecheckFails() {
        givenAvailableSpace(1);
        givenReservationAcquired();
        var failure = new IllegalStateException("Occupancy lookup failed");
        doThrow(failure).when(activeStays).findOccupiedSpacesByLocationId(LOCATION.id());

        assertThatThrownBy(this::park).isSameAs(failure);
        assertAcquiredReservationReleased();
        verify(activeStays, never()).tryAdd(any(ParkingStay.class));
    }

    @Test
    void releasesTheReservationWhenActiveInsertionFails() {
        givenAvailableSpace(1);
        givenReservationAcquired();
        givenSpaceUnoccupied();
        var failure = new IllegalStateException("Active insertion failed");
        doThrow(failure).when(activeStays).tryAdd(any(ParkingStay.class));

        assertThatThrownBy(this::park).isSameAs(failure);
        assertAcquiredReservationReleased();
    }

    private ParkingStay park() {
        return subject.parkVehicle(LOCATION.id(), REGISTRATION, VehicleType.SMALL);
    }

    private void givenOpenLocation() {
        doReturn(LOCATION).when(locationService).findOpenParkingLocationById(LOCATION.id());
    }

    private void givenAvailableSpace(int spaceNumber) {
        givenOpenLocation();
        doReturn(OptionalInt.of(spaceNumber)).when(activeStays)
                .findNextAvailableSpace(LOCATION.id(), LOCATION.maxCapacity());
    }

    private void givenReservationAcquired() {
        doReturn(Optional.empty()).when(reservations).tryReserve(any(ParkingStay.class));
    }

    private void givenSpaceUnoccupied() {
        doReturn(List.of()).when(activeStays).findOccupiedSpacesByLocationId(LOCATION.id());
    }

    private void givenRegistrationAccepted() {
        doReturn(Optional.empty()).when(activeStays).tryAdd(any(ParkingStay.class));
    }

    private void assertAcquiredReservationReleased() {
        verify(reservations).tryReserve(stayCaptor.capture());
        verify(reservations).release(same(stayCaptor.getValue()));
    }

    private static ParkingStay existingStay(long locationId, int spaceNumber) {
        return new ParkingStay(UUID.fromString("00000000-0000-0000-0000-000000000001"),
                locationId, spaceNumber, REGISTRATION, VehicleType.SMALL,
                EXPECTED_TIME.minusSeconds(60), null, null);
    }
}
