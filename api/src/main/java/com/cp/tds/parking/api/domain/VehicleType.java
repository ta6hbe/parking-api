package com.cp.tds.parking.api.domain;

/**
 * Maps the supported vehicle sizes to their numeric API codes.
 */
public enum VehicleType {
    SMALL(1),
    MEDIUM(2),
    LARGE(3);

    private final int code;

    VehicleType(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static VehicleType fromCode(int code) {
        return switch (code) {
            case 1 -> SMALL;
            case 2 -> MEDIUM;
            case 3 -> LARGE;
            default -> throw new IllegalArgumentException("Vehicle type must be 1, 2 or 3");
        };
    }
}
