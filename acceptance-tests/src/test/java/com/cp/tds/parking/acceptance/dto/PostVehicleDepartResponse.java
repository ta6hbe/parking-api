package com.cp.tds.parking.acceptance.dto;

import java.time.Instant;
import java.util.UUID;

public record PostVehicleDepartResponse(
        UUID parkingSessionId,
        String vehicleReg,
        Double vehicleCharge,
        Instant timeIn,
        Instant timeOut
) {}
