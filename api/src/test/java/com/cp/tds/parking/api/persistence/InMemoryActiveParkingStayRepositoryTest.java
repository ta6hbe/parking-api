package com.cp.tds.parking.api.persistence;

import com.cp.tds.parking.api.TestFixtures;
import com.cp.tds.parking.api.domain.ParkingStay;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Checks active-session counts by location and global vehicle registration uniqueness.
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
