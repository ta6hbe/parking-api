package com.cp.tds.parking.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Identifies the vehicle whose active parking session should be completed.
 */
public record PostVehicleDepartRequest(
        @Pattern(
                regexp = "\\s*(?:[A-Za-z]\\s*){1,2}(?:[0-9]\\s*){2,3}(?:[A-Za-z]\\s*){1,3}",
                message = "Vehicle registration must have 1–2 letters, 2–3 digits, then 1–3 letters"
        )
        @NotBlank String vehicleReg
) {
}
