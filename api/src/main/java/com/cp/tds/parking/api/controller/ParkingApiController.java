package com.cp.tds.parking.api.controller;

import com.cp.tds.parking.api.config.ParkingProperties;
import com.cp.tds.parking.api.dto.GetParkingResponse;
import com.cp.tds.parking.api.dto.PostParkingRequest;
import com.cp.tds.parking.api.dto.PostParkingResponse;
import com.cp.tds.parking.api.mapper.OccupancyMapper;
import com.cp.tds.parking.api.mapper.ParkingMapper;
import com.cp.tds.parking.api.domain.VehicleType;
import com.cp.tds.parking.api.service.ParkingService;
import com.cp.tds.parking.api.service.OccupancyService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/parking")
public class ParkingApiController {

    private final ParkingProperties parkingProperties;
    private final OccupancyService occupancyService;
    private final OccupancyMapper occupancyMapper;
    private final ParkingService parkingService;
    private final ParkingMapper parkingMapper;

    public ParkingApiController(
            ParkingProperties parkingProperties,
            OccupancyService occupancyService,
            OccupancyMapper occupancyMapper,
            ParkingService parkingService,
            ParkingMapper parkingMapper) {
        this.occupancyService = occupancyService;
        this.occupancyMapper = occupancyMapper;
        this.parkingProperties = parkingProperties;
        this.parkingService = parkingService;
        this.parkingMapper = parkingMapper;
    }

    @GetMapping
    public ResponseEntity<GetParkingResponse> getParking() {

        GetParkingResponse occupancy = occupancyMapper.toResponse(
                occupancyService.getOccupancy(parkingProperties.getDefaultLocationId()));

        return ResponseEntity.ok(occupancy);

    }

    @GetMapping("/{locationId}")
    public ResponseEntity<GetParkingResponse> getParkingByLocationId(
            @PathVariable Long locationId) {
        GetParkingResponse occupancy = occupancyMapper.toResponse(
                occupancyService.getOccupancy(locationId)
        );
        return ResponseEntity.ok(occupancy);
    }

    @PostMapping
    public ResponseEntity<PostParkingResponse>  postParkingAtDefaultLocation(
            @Valid @RequestBody PostParkingRequest postParkingRequest) {

        return parkVehicle(parkingProperties.getDefaultLocationId(), postParkingRequest);
    }

    @PostMapping("/{locationId}")
    public ResponseEntity<PostParkingResponse> postParkingAtLocation(
            @PathVariable long locationId,
            @Valid @RequestBody PostParkingRequest postParkingRequest) {
        return parkVehicle(locationId, postParkingRequest);
    }

    private ResponseEntity<PostParkingResponse> parkVehicle(long locationId, PostParkingRequest request) {
        var stay = parkingService.parkVehicle(
                locationId,
                request.vehicleReg(),
                VehicleType.fromCode(request.vehicleType()));
        return ResponseEntity.status(HttpStatus.CREATED).body(parkingMapper.toResponse(stay));
    }
}
