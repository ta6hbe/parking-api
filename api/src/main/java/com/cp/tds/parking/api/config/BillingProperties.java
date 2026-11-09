package com.cp.tds.parking.api.config;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Binds parking rates in integer pence and the additional charging interval.
 */
@Validated
@ConfigurationProperties(prefix = "billing")
public record BillingProperties(
        @Min(0) long smallRatePencePerMinute,
        @Min(0) long mediumRatePencePerMinute,
        @Min(0) long largeRatePencePerMinute,
        @Min(1) long intervalSeconds,
        @Min(0) long intervalChargePence
) {
}
