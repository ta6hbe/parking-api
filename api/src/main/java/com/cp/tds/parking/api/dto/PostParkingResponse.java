package com.cp.tds.parking.api.dto;

public record PostParkingResponse(
        String parkingSessionId,
        String vehicleReg,
        long spaceNumber,
        String timeArrive) {}
