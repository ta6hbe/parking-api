package com.cp.tds.parking.api.mapper;

import com.cp.tds.parking.api.domain.ParkingLocationOccupancy;
import com.cp.tds.parking.api.dto.GetParkingResponse;
import org.springframework.stereotype.Component;

/**
 * Converts domain occupancy observations into API response objects.
 */
@Component
public class OccupancyMapper {

    public GetParkingResponse toResponse(ParkingLocationOccupancy occupancy) {
        return new GetParkingResponse(
                occupancy.locationId(),
                occupancy.available(),
                occupancy.occupied(),
                occupancy.timeChecked()
        );
    }
}
