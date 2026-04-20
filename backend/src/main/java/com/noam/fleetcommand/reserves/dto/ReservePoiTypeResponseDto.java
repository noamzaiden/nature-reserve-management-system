package com.noam.fleetcommand.reserves.dto;

import java.time.LocalDateTime;

public record ReservePoiTypeResponseDto(
        Long id,
        Long reserveId,
        String name,
        boolean systemDefault,
        LocalDateTime createdAt
) {
}
