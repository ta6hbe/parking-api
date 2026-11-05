package com.cp.tds.parking.acceptance.dto;

/**
 * Represents the public Problem Details contract independently of application DTOs.
 */
public record ProblemResponse(int status, String title, String detail) {
}
