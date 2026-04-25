package com.noam.fleetcommand.reserves.dto;

public record ReserveSuggestionDto(
        Long id,
        String name,
        String displayName,
        String region
) {
}
