package com.cp.tds.parking.api.persistence;

import com.cp.tds.parking.api.domain.ParkingStay;
import com.cp.tds.parking.api.domain.VehicleType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Checks UUID-keyed history insertion and conditional removal.
 */
class InMemoryParkingStayHistoryRepositoryTest {

    private InMemoryParkingStayHistoryRepository subject;

    @BeforeEach
    void setUp() {
        subject = new InMemoryParkingStayHistoryRepository();
    }

    @Test
    void storesAndFindsACompletedStay() {
        ParkingStay stay = completedStay(UUID.randomUUID(), "AB12CDE");

        assertThat(subject.tryAdd(stay))
                .as("completed stay is inserted")
                .isEmpty();

        assertThat(subject.findByParkingSessionId(stay.parkingSessionId()))
                .as("stored completed stay")
                .contains(stay);
    }

    @Test
    void returnsEmptyForAnUnknownSession() {
        assertThat(subject.findByParkingSessionId(UUID.randomUUID()))
                .as("no history for an unknown session")
                .isEmpty();
    }

    @Test
    void rejectsDuplicateSessionWithoutReplacingTheOriginal() {
        UUID sessionId = UUID.randomUUID();
        ParkingStay original = completedStay(sessionId, "AB12CDE");
        ParkingStay conflicting = completedStay(sessionId, "XY34FGH");

        assertThat(subject.tryAdd(original)).isEmpty();

        assertThat(subject.tryAdd(conflicting))
                .as("duplicate insertion returns the original stay")
                .contains(original);

        assertThat(subject.findByParkingSessionId(sessionId))
                .as("original history remains unchanged")
                .contains(original);
    }

    @Test
    void retainsDifferentSessionsForTheSameRegistration() {
        ParkingStay first = completedStay(UUID.randomUUID(), "AB12CDE");
        ParkingStay second = completedStay(UUID.randomUUID(), "AB12CDE");

        assertThat(subject.tryAdd(first)).isEmpty();
        assertThat(subject.tryAdd(second)).isEmpty();

        assertThat(subject.findByParkingSessionId(first.parkingSessionId()))
                .contains(first);
        assertThat(subject.findByParkingSessionId(second.parkingSessionId()))
                .contains(second);
    }

    @Test
    void removesOnlyTheMatchingCompletedStay() {
        ParkingStay stay = completedStay(UUID.randomUUID(), "AB12CDE");
        ParkingStay other = completedStay(UUID.randomUUID(), "XY34FGH");

        assertThat(subject.tryAdd(stay)).isEmpty();
        assertThat(subject.tryAdd(other)).isEmpty();

        assertThat(subject.remove(stay))
                .as("matching history insertion is removed")
                .isTrue();

        assertThat(subject.findByParkingSessionId(stay.parkingSessionId()))
                .isEmpty();
        assertThat(subject.findByParkingSessionId(other.parkingSessionId()))
                .as("another departure's history is untouched")
                .contains(other);
    }

    @Test
    void doesNotRemoveADifferentStayWithTheSameSessionId() {
        UUID sessionId = UUID.randomUUID();
        ParkingStay original = completedStay(sessionId, "AB12CDE");
        ParkingStay different = completedStay(sessionId, "XY34FGH");

        assertThat(subject.tryAdd(original)).isEmpty();

        assertThat(subject.remove(different))
                .as("a different value cannot remove existing history")
                .isFalse();

        assertThat(subject.findByParkingSessionId(sessionId))
                .contains(original);
    }

    @Test
    void returnsFalseWhenRemovingAnAbsentStay() {
        assertThat(subject.remove(
                completedStay(UUID.randomUUID(), "AB12CDE")))
                .as("absent history cannot be removed")
                .isFalse();
    }

    private ParkingStay completedStay(UUID sessionId, String registration) {
        Instant timeIn = Instant.parse("2026-11-08T12:00:00Z");

        return new ParkingStay(
                sessionId,
                1L,
                1,
                registration,
                VehicleType.SMALL,
                timeIn,
                timeIn.plus(Duration.ofMinutes(5)),
                new BigDecimal("1.50"));
    }
}
