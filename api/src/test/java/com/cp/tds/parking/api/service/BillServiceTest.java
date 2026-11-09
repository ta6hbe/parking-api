package com.cp.tds.parking.api.service;

import com.cp.tds.parking.api.config.BillingProperties;
import com.cp.tds.parking.api.domain.ParkingStay;
import com.cp.tds.parking.api.domain.VehicleType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Checks parking charges through the public billing service.
 */
class BillServiceTest {

    private static final Instant TIME_IN =
            Instant.parse("2026-11-08T12:00:00Z");

    private final BillService subject =
            new BillService(new BillingProperties(10, 20, 40, 300, 100));

    @ParameterizedTest
    @MethodSource("chargeCases")
    void calculatesCharge(
            VehicleType vehicleType,
            long seconds,
            String expectedCharge) {

        assertThat(subject.calculateCharge(
                activeStay(vehicleType), TIME_IN.plusSeconds(seconds)))
                .as("charge for %s after %s seconds", vehicleType, seconds)
                .isEqualByComparingTo(expectedCharge);
    }

    @Test
    void ignoresFractionalSeconds() {
        Instant calculationTime = TIME_IN.plus(
                Duration.ofMinutes(4)
                        .plusSeconds(59)
                        .plusNanos(999_999_999));

        assertThat(subject.calculateCharge(
                activeStay(VehicleType.SMALL), calculationTime))
                .isEqualByComparingTo("0.49");
    }

    @Test
    void rejectsDepartureBeforeArrivalIncludingFractionalSeconds() {
        assertThatThrownBy(() -> subject.calculateCharge(
                activeStay(VehicleType.SMALL), TIME_IN.minusNanos(1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Departure time must not be before entry time");
    }

    @Test
    void detectsArithmeticOverflow() {
        BillService overflowingSubject = new BillService(
                new BillingProperties(Long.MAX_VALUE, 20, 40, 300, 100));

        assertThatThrownBy(() -> overflowingSubject.calculateCharge(
                activeStay(VehicleType.SMALL), TIME_IN.plusSeconds(2)))
                .isInstanceOf(ArithmeticException.class);
    }

    // Vehicle size, elapsed seconds expressed as hours * 3600 + minutes * 60 + seconds,
    // and expected charge in pounds.
    private static List<Arguments> chargeCases() {
        return List.of(
                Arguments.of(VehicleType.SMALL,  0 * 3600L +  0 * 60 +  0, "0.00"),
                Arguments.of(VehicleType.SMALL,  0 * 3600L +  0 * 60 + 59, "0.09"),
                Arguments.of(VehicleType.SMALL,  0 * 3600L +  1 * 60 +  0, "0.10"),
                Arguments.of(VehicleType.SMALL,  0 * 3600L +  4 * 60 + 59, "0.49"),
                Arguments.of(VehicleType.SMALL,  0 * 3600L +  5 * 60 +  0, "1.50"),
                Arguments.of(VehicleType.SMALL,  0 * 3600L +  9 * 60 + 59, "1.99"),
                Arguments.of(VehicleType.SMALL,  0 * 3600L + 10 * 60 +  0, "3.00"),
                Arguments.of(VehicleType.MEDIUM, 0 * 3600L +  5 * 60 +  0, "2.00"),
                Arguments.of(VehicleType.LARGE,  0 * 3600L +  5 * 60 +  0, "3.00"),
                Arguments.of(VehicleType.SMALL, 25 * 3600L +  0 * 60 +  0, "450.00"));
    }

    private ParkingStay activeStay(VehicleType vehicleType) {
        return new ParkingStay(
                UUID.randomUUID(),
                1L,
                1,
                "AB12CDE",
                vehicleType,
                TIME_IN,
                null,
                null);
    }
}
