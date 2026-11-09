package com.cp.tds.parking.api.mapper;

import com.cp.tds.parking.api.domain.ParkingStay;
import com.cp.tds.parking.api.dto.PostVehicleParkResponse;
import org.springframework.stereotype.Component;

/**
 * Converts an active parking stay into its parking response representation.
 */
@Component
public class ParkingMapper {

    public PostVehicleParkResponse toResponse(ParkingStay stay) {
        return new PostVehicleParkResponse(
                stay.parkingSessionId(),
                stay.vehicleRegistration(),
                stay.spaceNumber(),
                stay.timeIn());
    }
}
