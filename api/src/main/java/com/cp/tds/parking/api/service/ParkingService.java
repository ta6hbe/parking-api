package com.cp.tds.parking.api.service;

import com.cp.tds.parking.api.config.ParkingProperties;
import com.cp.tds.parking.api.domain.ParkingStay;
import com.cp.tds.parking.api.domain.VehicleType;
import com.cp.tds.parking.api.persistence.ActiveParkingStayRepositoryI;
import org.springframework.stereotype.Service;

import java.time.Clock;

@Service
public class ParkingService {

    private final ParkingProperties parkingProperties;
    private final ActiveParkingStayRepositoryI activeParkingStayRepository;
    private final Clock parkingClock;

    public ParkingService(
            ParkingProperties parkingProperties,
            ActiveParkingStayRepositoryI activeParkingStayRepository,
            Clock parkingClock) {
        this.parkingProperties = parkingProperties;
        this.activeParkingStayRepository = activeParkingStayRepository;
        this.parkingClock = parkingClock;
    }

    // needs to lock to protect from concurrency. Also transactional boundary here.
    public ParkingStay parkVehicle(
            long locationId,
            String vehicleRegistration,
            VehicleType vehicleType
    ) {
        // not implemented
        return null;
    }
}
