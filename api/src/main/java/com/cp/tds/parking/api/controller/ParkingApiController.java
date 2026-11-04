package com.cp.tds.parking.api.controller;

import com.cp.tds.parking.api.config.ParkingProperties;
import com.cp.tds.parking.api.dto.GetParkingResponse;
import com.cp.tds.parking.api.mapper.OccupancyMapper;
import com.cp.tds.parking.api.service.OccupancyService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/parking")
public class ParkingApiController {

    private final ParkingProperties parkingProperties;
    private final OccupancyService occupancyService;
    private final OccupancyMapper occupancyMapper;

    public ParkingApiController(
            ParkingProperties parkingProperties,
            OccupancyService occupancyService,
            OccupancyMapper occupancyMapper) {
        this.occupancyService = occupancyService;
        this.occupancyMapper = occupancyMapper;
        this.parkingProperties = parkingProperties;
    }

    @GetMapping
    public ResponseEntity<GetParkingResponse> getParking() {

        GetParkingResponse occupancy = occupancyMapper.toResponse(
                occupancyService.getOccupancy(parkingProperties.getDefaultLocationId()));

        return ResponseEntity.ok(occupancy);

    }
}
