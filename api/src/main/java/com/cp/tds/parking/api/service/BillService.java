package com.cp.tds.parking.api.service;

import com.cp.tds.parking.api.config.BillingProperties;
import com.cp.tds.parking.api.domain.ParkingStay;
import com.cp.tds.parking.api.domain.VehicleType;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;

/**
 * Calculates parking charges using whole elapsed seconds and integer pence.
 * Integer arithmetic avoids floating-point rounding errors; the final amount
 * is represented exactly in pounds using BigDecimal.
 */
@Service
public class BillService {

    private final BillingProperties properties;

    public BillService(BillingProperties properties) {
        this.properties = properties;
    }

    /**
     * Calculates a charge without changing the supplied stay.
     *
     * @throws IllegalArgumentException if calculation time precedes entry time
     * @throws ArithmeticException if charge arithmetic overflows
     */
    public BigDecimal calculateCharge(
            ParkingStay stay, Instant calculationTime) {

        Duration elapsed = Duration.between(stay.timeIn(), calculationTime);

        // Reject negative durations before discarding fractional seconds.
        if (elapsed.isNegative()) {
            throw new IllegalArgumentException(
                    "Departure time must not be before entry time");
        }

        long seconds = elapsed.getSeconds();

        long basePence = Math.multiplyExact(
                seconds, ratePencePerMinute(stay.vehicleType())) / 60;

        long surchargePence = Math.multiplyExact(
                seconds / properties.intervalSeconds(),
                properties.intervalChargePence());

        long totalPence = Math.addExact(basePence, surchargePence);

        return BigDecimal.valueOf(totalPence, 2);
    }

    private long ratePencePerMinute(VehicleType vehicleType) {
        return switch (vehicleType) {
            case SMALL -> properties.smallRatePencePerMinute();
            case MEDIUM -> properties.mediumRatePencePerMinute();
            case LARGE -> properties.largeRatePencePerMinute();
        };
    }
}
