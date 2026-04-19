package com.noam.fleetcommand.reserves.dto;

import java.time.LocalDateTime;

public record ReservePoiResponseDto(
        Long id,
        Long reserveId,
        Long typeId,
        String typeName,
        String name,
        String description,
        Double latitude,
        Double longitude,
        LocalDateTime createdAt
) {
}
