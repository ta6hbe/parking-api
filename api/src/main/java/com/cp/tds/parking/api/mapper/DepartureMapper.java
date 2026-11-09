package com.cp.tds.parking.api.mapper;

import com.cp.tds.parking.api.domain.ParkingStay;
import com.cp.tds.parking.api.dto.PostVehicleDepartResponse;
import org.springframework.stereotype.Component;

/**
 * Converts a completed parking stay into its public final-bill response.
 */
@Component
public class DepartureMapper {

    public PostVehicleDepartResponse toResponse(ParkingStay completedStay) {
        return new PostVehicleDepartResponse(
                completedStay.parkingSessionId(),
                completedStay.vehicleRegistration(),
                completedStay.finalCharge().doubleValue(),
                completedStay.timeIn(),
                completedStay.timeOut());
    }
}
