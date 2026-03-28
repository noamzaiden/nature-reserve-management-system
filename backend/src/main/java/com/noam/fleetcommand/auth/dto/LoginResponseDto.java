package com.noam.fleetcommand.auth.dto;

public record LoginResponseDto(String token, String role, Long userId) {
}
