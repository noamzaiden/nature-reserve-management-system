package com.noam.fleetcommand.requests.dto;

import java.time.LocalDateTime;

public record ReserveCreationRequestResponseDto(
        Long id,
        String reserveName,
        String message,
        String status,
        Long requestedByUserId,
        String requestedByName,
        String requestedByEmail,
        LocalDateTime createdAt,
        LocalDateTime resolvedAt
) {
}
