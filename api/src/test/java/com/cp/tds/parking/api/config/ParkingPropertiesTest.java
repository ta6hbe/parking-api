package com.cp.tds.parking.api.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Checks that the default location configuration is loaded and bound correctly.
 */
@SpringBootTest(webEnvironment = NONE)
public class ParkingPropertiesTest {

    @Autowired
    ParkingProperties properties;

    @Test
    void loadsDefaultParkingLocation() {
        assertThat(properties.getDefaultLocationId())
                .as("default location ID")
                .isEqualTo(1L);

        assertThat(properties.getLocations())
                .as("Configured parking locations")
                .singleElement()
                .satisfies(location -> {
                    assertThat(location.id()).isEqualTo(1L);
                    assertThat(location.name()).isEqualTo("TDS Car Park 1");
                    assertThat(location.maxCapacity()).isEqualTo(10);
                    assertThat(location.status().name()).hasToString("ACTIVE");
                });
    }
}
