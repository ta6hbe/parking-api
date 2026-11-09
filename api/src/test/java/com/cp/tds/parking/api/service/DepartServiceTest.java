package com.cp.tds.parking.api.service;

import com.cp.tds.parking.api.domain.ParkingStay;
import com.cp.tds.parking.api.domain.VehicleType;
import com.cp.tds.parking.api.exception.ActiveStayNotFoundException;
import com.cp.tds.parking.api.exception.DepartureConflictException;
import com.cp.tds.parking.api.persistence.ActiveParkingStayRepositoryI;
import com.cp.tds.parking.api.persistence.LocationSpaceReservationRepositoryI;
import com.cp.tds.parking.api.persistence.ParkingStayHistoryRepositoryI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Checks departure results, rejection, and failure cleanup through the public service.
 * Mocked conflicts exercise service decisions, not real concurrent execution.
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
    void returnsCompletedStayAfterStoringHistoryRemovingActiveStayAndReleasingReservation() {
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

    @Test
    void rejectsDepartureWithoutAcquiringReservationWhenNoActiveStayExists() {
        when(activeStays.findByVehicleRegistration(REGISTRATION))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> subject.departVehicle(" ab 12 cde "))
                .isInstanceOf(ActiveStayNotFoundException.class)
                .hasMessage("Active stay not found for vehicle registration: " + REGISTRATION);

        verify(activeStays).findByVehicleRegistration(REGISTRATION);
        verifyNoMoreInteractions(activeStays);
        verifyNoInteractions(reservations, billService, history);
    }

    @Test
    void rejectsDepartureWithoutRetryingOrReleasingReservationWhenLockAcquisitionConflicts() {
        ParkingStay active = activeStay();
        // The competing reservation shares the location-space, not the vehicle or session.
        ParkingStay competingReservation = new ParkingStay(
                UUID.randomUUID(), active.locationId(), active.spaceNumber(),
                "XY34FGH", VehicleType.LARGE, TIME_OUT, null, null);
        when(activeStays.findByVehicleRegistration(REGISTRATION))
                .thenReturn(Optional.of(active));
        when(reservations.tryReserve(active))
                .thenReturn(Optional.of(competingReservation));

        assertThatThrownBy(() -> subject.departVehicle(REGISTRATION))
                .isInstanceOf(DepartureConflictException.class)
                .hasMessage("The parking session cannot be completed at this time "
                        + "because of a concurrent operation. Please try again.");

        verify(reservations).tryReserve(active);
        verifyNoMoreInteractions(reservations);
        verify(activeStays).findByVehicleRegistration(REGISTRATION);
        verifyNoMoreInteractions(activeStays);
        verifyNoInteractions(billService, history);
    }

    @Test
    void rejectsDepartureAndReleasesReservationWhenRecheckFindsNoActiveStay() {
        ParkingStay active = activeStay();
        when(activeStays.findByVehicleRegistration(REGISTRATION))
                .thenReturn(Optional.of(active), Optional.empty());
        when(reservations.tryReserve(active)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> subject.departVehicle(REGISTRATION))
                .isInstanceOf(ActiveStayNotFoundException.class)
                .hasMessage("Active stay not found for vehicle registration: " + REGISTRATION);

        verifyRejectedRecheckOrder(active);
        verify(activeStays, never()).remove(any(ParkingStay.class));
        verifyNoInteractions(billService, history);
    }

    @Test
    void rejectsDepartureAndReleasesOriginalReservationWhenRecheckFindsADifferentSession() {
        ParkingStay active = activeStay();
        ParkingStay replacement = new ParkingStay(
                UUID.randomUUID(), active.locationId(), active.spaceNumber(),
                REGISTRATION, active.vehicleType(), TIME_OUT, null, null);
        when(activeStays.findByVehicleRegistration(REGISTRATION))
                .thenReturn(Optional.of(active), Optional.of(replacement));
        when(reservations.tryReserve(active)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> subject.departVehicle(REGISTRATION))
                .isInstanceOf(ActiveStayNotFoundException.class)
                .hasMessage("Active stay not found for vehicle registration: " + REGISTRATION);

        verifyRejectedRecheckOrder(active);
        verify(activeStays, never()).remove(any(ParkingStay.class));
        verify(reservations, never()).release(replacement);
        verifyNoInteractions(billService, history);
    }

    @ParameterizedTest
    @MethodSource("billingFailures")
    void releasesReservationWithoutRemovingActiveStayOrWritingHistoryWhenBillingFails(
            RuntimeException failure) {
        ParkingStay active = activeStay();
        givenActiveStayAndSuccessfulReservation(active);
        when(billService.calculateCharge(active, TIME_OUT)).thenThrow(failure);

        assertThatThrownBy(() -> subject.departVehicle(REGISTRATION))
                .isSameAs(failure);

        verify(activeStays, never()).remove(any(ParkingStay.class));
        verifyNoInteractions(history);
        verify(reservations).release(active);
        assertThat(active).extracting("timeOut", "finalCharge")
                .containsOnlyNulls();
    }

    @Test
    void rejectsDepartureWithoutRemovingActiveStayOrExistingHistoryWhenHistoryInsertionConflicts() {
        ParkingStay active = activeStay();
        ParkingStay completed = completedStay(active);
        givenActiveStayAndSuccessfulReservation(active);
        when(billService.calculateCharge(active, TIME_OUT)).thenReturn(CHARGE);
        when(history.tryAdd(completed)).thenReturn(Optional.of(completed));

        assertThatThrownBy(() -> subject.departVehicle(REGISTRATION))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("History already exists for parking session: "
                        + active.parkingSessionId());

        verify(history).tryAdd(completed);
        verifyNoMoreInteractions(history);
        verify(activeStays, never()).remove(any(ParkingStay.class));
        verify(reservations).release(active);
    }

    @Test
    void releasesReservationWithoutRemovingActiveStayOrDeletingHistoryWhenHistoryInsertionThrows() {
        ParkingStay active = activeStay();
        ParkingStay completed = completedStay(active);
        RuntimeException failure = new IllegalStateException("History insertion failed");
        givenActiveStayAndSuccessfulReservation(active);
        when(billService.calculateCharge(active, TIME_OUT)).thenReturn(CHARGE);
        when(history.tryAdd(completed)).thenThrow(failure);

        assertThatThrownBy(() -> subject.departVehicle(REGISTRATION))
                .isSameAs(failure);

        verify(history).tryAdd(completed);
        verifyNoMoreInteractions(history);
        verify(activeStays, never()).remove(any(ParkingStay.class));
        verify(reservations).release(active);
    }

    @Test
    void removesInsertedHistoryAndReleasesReservationWhenActiveRemovalReturnsFalse() {
        ParkingStay active = activeStay();
        ParkingStay completed = completedStay(active);
        givenSuccessfulHistoryInsertion(active);
        when(activeStays.remove(active)).thenReturn(false);
        when(history.remove(completed)).thenReturn(true);

        assertThatThrownBy(() -> subject.departVehicle(REGISTRATION))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Could not remove the protected active parking session: "
                        + active.parkingSessionId())
                .hasNoSuppressedExceptions();

        verifyHistoryCleanupOrder(active, completed);
        verify(activeStays, never()).tryAdd(any(ParkingStay.class));
    }

    @Test
    void removesInsertedHistoryAndReleasesReservationWhenActiveRemovalThrowsBeforeMutation() {
        ParkingStay active = activeStay();
        ParkingStay completed = completedStay(active);
        RuntimeException failure = new IllegalStateException("Active removal failed before mutation");
        givenSuccessfulHistoryInsertion(active);
        when(activeStays.remove(active)).thenThrow(failure);
        when(history.remove(completed)).thenReturn(true);

        assertThatThrownBy(() -> subject.departVehicle(REGISTRATION))
                .isSameAs(failure)
                .hasNoSuppressedExceptions();

        verifyHistoryCleanupOrder(active, completed);
        verify(activeStays, never()).tryAdd(any(ParkingStay.class));
    }

    @Test
    void rethrowsActiveRemovalFailureWithSuppressedErrorWhenHistoryRemovalReturnsFalse() {
        ParkingStay active = activeStay();
        ParkingStay completed = completedStay(active);
        RuntimeException failure = new IllegalStateException("Active removal failed before mutation");
        givenSuccessfulHistoryInsertion(active);
        when(activeStays.remove(active)).thenThrow(failure);
        when(history.remove(completed)).thenReturn(false);

        assertThatThrownBy(() -> subject.departVehicle(REGISTRATION))
                .isSameAs(failure);
        assertThat(failure.getSuppressed()).singleElement()
                .isInstanceOfSatisfying(IllegalStateException.class, cleanupFailure ->
                        assertThat(cleanupFailure).hasMessage(
                                "Could not undo history insertion for parking session: "
                                        + active.parkingSessionId()));

        verifyHistoryCleanupOrder(active, completed);
    }

    @Test
    void rethrowsActiveRemovalFailureWithSuppressedCleanupExceptionWhenHistoryRemovalThrows() {
        ParkingStay active = activeStay();
        ParkingStay completed = completedStay(active);
        RuntimeException failure = new IllegalStateException("Active removal failed before mutation");
        RuntimeException cleanupFailure = new IllegalStateException("History cleanup failed");
        givenSuccessfulHistoryInsertion(active);
        when(activeStays.remove(active)).thenThrow(failure);
        when(history.remove(completed)).thenThrow(cleanupFailure);

        assertThatThrownBy(() -> subject.departVehicle(REGISTRATION))
                .isSameAs(failure)
                .hasSuppressedException(cleanupFailure);

        verifyHistoryCleanupOrder(active, completed);
    }

    @Test
    void propagatesReleaseFailureWithoutRemovingHistoryOrReinsertingActiveStayAfterCommit() {
        ParkingStay active = activeStay();
        ParkingStay completed = completedStay(active);
        RuntimeException failure = new IllegalStateException("Reservation release failed");
        givenSuccessfulHistoryInsertion(active);
        when(activeStays.remove(active)).thenReturn(true);
        when(reservations.release(active)).thenThrow(failure);

        assertThatThrownBy(() -> subject.departVehicle(REGISTRATION))
                .isSameAs(failure);

        var commitOrder = inOrder(history, activeStays, reservations);
        commitOrder.verify(history).tryAdd(completed);
        commitOrder.verify(activeStays).remove(active);
        commitOrder.verify(reservations).release(active);
        verify(history, never()).remove(any(ParkingStay.class));
        verify(activeStays, never()).tryAdd(any(ParkingStay.class));
        verify(reservations, times(1)).release(active);
    }

    // Supplies mocked negative-duration and overflow failures from billing, before any history insertion.
    private static List<Arguments> billingFailures() {
        return List.of(
                Arguments.of(new IllegalArgumentException("Departure time must not be before entry time")),
                Arguments.of(new ArithmeticException("long overflow")));
    }

    private void givenActiveStayAndSuccessfulReservation(ParkingStay active) {
        when(activeStays.findByVehicleRegistration(REGISTRATION))
                .thenReturn(Optional.of(active));
        when(reservations.tryReserve(active)).thenReturn(Optional.empty());
    }

    private void givenSuccessfulHistoryInsertion(ParkingStay active) {
        givenActiveStayAndSuccessfulReservation(active);
        when(billService.calculateCharge(active, TIME_OUT)).thenReturn(CHARGE);
        when(history.tryAdd(completedStay(active))).thenReturn(Optional.empty());
    }

    private void verifyRejectedRecheckOrder(ParkingStay active) {
        var rejectionOrder = inOrder(activeStays, reservations);
        rejectionOrder.verify(activeStays).findByVehicleRegistration(REGISTRATION);
        rejectionOrder.verify(reservations).tryReserve(active);
        rejectionOrder.verify(activeStays).findByVehicleRegistration(REGISTRATION);
        rejectionOrder.verify(reservations).release(active);
        verify(reservations, times(1)).release(active);
    }

    private void verifyHistoryCleanupOrder(ParkingStay active, ParkingStay completed) {
        var cleanupOrder = inOrder(reservations, history, activeStays);
        cleanupOrder.verify(reservations).tryReserve(active);
        cleanupOrder.verify(history).tryAdd(completed);
        cleanupOrder.verify(activeStays).remove(active);
        cleanupOrder.verify(history).remove(completed);
        cleanupOrder.verify(reservations).release(active);
        verify(history, times(1)).remove(completed);
        verifyNoMoreInteractions(history);
        verify(reservations, times(1)).release(active);
    }

    private ParkingStay completedStay(ParkingStay active) {
        return new ParkingStay(
                active.parkingSessionId(), active.locationId(), active.spaceNumber(),
                active.vehicleRegistration(), active.vehicleType(), active.timeIn(),
                TIME_OUT, CHARGE);
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
