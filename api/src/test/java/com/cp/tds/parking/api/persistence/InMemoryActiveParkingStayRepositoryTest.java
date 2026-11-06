package com.cp.tds.parking.api.persistence;

import com.cp.tds.parking.api.TestFixtures;
import com.cp.tds.parking.api.domain.ParkingStay;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Checks session counts, registration uniqueness, and lowest-free-space lookup.
 */
class InMemoryActiveParkingStayRepositoryTest {

    private InMemoryActiveParkingStayRepository subject;

    @BeforeEach
    void setUp() {
        subject = new InMemoryActiveParkingStayRepository();
    }

    @Test
    void returnsZeroWhenThereAreNoActiveSessions() {
        assertThat(subject.countByLocationId(TestFixtures.PARKING_LOCATION_1.id()))
                .as("active sessions at an empty location")
                .isZero();
    }

    @Test
    void countsOnlySessionsForTheRequestedLocation() {
        givenActiveStays(List.of(
                TestFixtures.ACTIVE_STAY_L1_1,
                TestFixtures.ACTIVE_STAY_L1_2,
                TestFixtures.ACTIVE_STAY_L2_1
        ));

        assertThat(subject.countByLocationId(TestFixtures.PARKING_LOCATION_1.id()))
                .as("active sessions at location 1")
                .isEqualTo(2L);

        assertThat(subject.countByLocationId(TestFixtures.PARKING_LOCATION_2.id()))
                .as("active sessions at location 2")
                .isEqualTo(1L);

        assertThat(subject.countByLocationId(99L))
                .as("active sessions at a location with no matching records")
                .isZero();
    }

    @Test
    void rejectsDuplicateRegistrationAcrossLocationsWithoutReplacingTheOriginal() {
        givenActiveStays(List.of(TestFixtures.ACTIVE_STAY_L1_1));
        ParkingStay duplicate = duplicateRegistrationAtAnotherLocation();

        assertThat(subject.tryAdd(duplicate))
                .as("same registration cannot have another active session")
                .isFalse();

        assertThat(subject.countByLocationId(TestFixtures.PARKING_LOCATION_1.id()))
                .as("original session remains at location 1")
                .isEqualTo(1L);

        assertThat(subject.countByLocationId(TestFixtures.PARKING_LOCATION_2.id()))
                .as("duplicate session is not stored at location 2")
                .isZero();
    }

    // Seed known occupancy directly so setup does not depend on the lookup under test.
    // Departure cases represent the stays left behind, without exercising removal or concurrency.
    @ParameterizedTest(name = "{0}")
    @MethodSource("minimumFreeSpaceCases")
    void findsMinimumFreeSpaceForOccupancyPattern(String description, List<Integer> occupiedSpaces, int expectedSpace) {
        givenOccupiedSpaces(1L, occupiedSpaces);

        assertThat(subject.findNextAvailableSpace(1L, 10))
                .as(description)
                .hasValue(expectedSpace);
    }

    @Test
    void returnsEmptyWhenEverySpaceIsOccupied() {
        // Unordered insertion also checks that lookup does not depend on fixture order.
        givenOccupiedSpaces(1L, List.of(10, 4, 1, 9, 2, 7, 5, 3, 6, 8));

        assertThat(subject.findNextAvailableSpace(1L, 10))
                .as("no free space in a full location")
                .isEmpty();
    }

    @Test
    void findsMinimumFreeSpaceUsingOnlyTheRequestedLocation() {
        // A space number occupied elsewhere must remain available at the requested location.
        givenOccupiedSpaces(1L, List.of(1, 3));
        givenOccupiedSpaces(2L, List.of(2));

        assertThat(subject.findNextAvailableSpace(1L, 3))
                .as("space 2 at another location does not fill the local gap")
                .hasValue(2);
        assertThat(subject.findNextAvailableSpace(2L, 3))
                .as("space 1 at another location does not occupy the local entrance space")
                .hasValue(1);
    }

    // Each row supplies a description, occupied space numbers, and the expected lowest free space.
    private static Stream<Arguments> minimumFreeSpaceCases() {
        return Stream.of(
                Arguments.of("returns space 1 when the location is empty", List.<Integer>of(), 1),
                Arguments.of("returns the final space when all preceding spaces are occupied", List.of(1, 2, 3, 4, 5, 6, 7, 8, 9), 10),
                Arguments.of("returns space 1 when a single occupied space is away from the start", List.of(5), 1),
                Arguments.of("returns the space after a contiguous occupied block starting at space 1", List.of(1, 2, 3, 4), 5),
                Arguments.of("returns the first gap between two contiguous occupied blocks starting at space 1", List.of(1, 2, 3, 6, 7, 8), 4),
                Arguments.of("returns space 1 when multiple occupied blocks leave both boundary spaces free", List.of(2, 3, 5, 6, 8), 1)
        );
    }

    private void givenOccupiedSpaces(long locationId, List<Integer> spaceNumbers) {
        givenActiveStays(spaceNumbers.stream()
                .map(space -> stayAt(locationId, space))
                .toList());
    }

    private ParkingStay stayAt(long locationId, int spaceNumber) {
        return new ParkingStay(
                new UUID(locationId, spaceNumber),
                locationId,
                spaceNumber,
                "L" + locationId + "SPACE" + spaceNumber,
                TestFixtures.ACTIVE_STAY_L1_1.vehicleType(),
                Instant.parse("2026-11-05T12:00:00Z"),
                null,
                null
        );
    }

    private void givenActiveStays(List<ParkingStay> stays) {
        for (ParkingStay stay : stays) {
            assertThat(subject.tryAdd(stay))
                    .as("fixture session for registration %s is accepted",
                            stay.vehicleRegistration())
                    .isTrue();
        }
    }

    private ParkingStay duplicateRegistrationAtAnotherLocation() {
        ParkingStay original = TestFixtures.ACTIVE_STAY_L1_1;

        return new ParkingStay(
                UUID.randomUUID(),
                TestFixtures.PARKING_LOCATION_2.id(),
                1,
                original.vehicleRegistration(),
                original.vehicleType(),
                original.timeIn(),
                null,
                null
        );
    }
}
