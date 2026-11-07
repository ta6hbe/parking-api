package com.cp.tds.parking.api.config;

import com.cp.tds.parking.api.domain.ParkingLocation;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Binds the default location ID and the locations configured under the parking prefix.
 */
@ConfigurationProperties(prefix = "parking")
public class ParkingProperties {

    int maxReservationRetryAttempts;
    private long defaultLocationId;
    private List<ParkingLocation> locations = List.of();


    public void setDefaultLocationId(long defaultLocationId) {
        this.defaultLocationId = defaultLocationId;
    }

    public long getDefaultLocationId() {
        return defaultLocationId;
    }


    public void  setLocations(List<ParkingLocation> locations) {
        this.locations = List.copyOf(locations);
    }

    public List<ParkingLocation> getLocations() {
        return locations;
    }

    public int getMaxReservationRetryAttempts() {
        return maxReservationRetryAttempts;
    }

    public void setMaxReservationRetryAttempts(int maxReservationRetryAttempts) {
        this.maxReservationRetryAttempts = maxReservationRetryAttempts;
    }
}
