package com.cp.tds.parking.api.service;

import com.cp.tds.parking.api.TestFixtures;
import com.cp.tds.parking.api.config.ParkingProperties;
import com.cp.tds.parking.api.exception.LocationNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Checks configured location resolution independently of active parking stays.
 */
class LocationServiceTest {

    private LocationService subject;

    @BeforeEach
    void setUp() {
        var properties = new ParkingProperties();
        properties.setLocations(TestFixtures.LOCATIONS_LIST);
        subject = new LocationService(properties);
    }

    @Test
    void returnsTheRequestedConfiguredLocation() {
        assertThat(subject.findLocationById(TestFixtures.PARKING_LOCATION_2.id()))
                .isEqualTo(TestFixtures.PARKING_LOCATION_2);
    }

    @Test
    void rejectsUnknownLocationWithItsIdInTheMessage() {
        assertThatThrownBy(() -> subject.findLocationById(999L))
                .isInstanceOf(LocationNotFoundException.class)
                .hasMessageContaining("999");
    }
}
