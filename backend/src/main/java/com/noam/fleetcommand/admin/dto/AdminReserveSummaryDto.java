package com.noam.fleetcommand.admin.dto;

import com.noam.fleetcommand.reserves.dto.AreaDto;

import java.time.LocalDateTime;

public record AdminReserveSummaryDto(
        Long id,
        String name,
        String displayName,
        String region,
        AreaDto area,
        Long managerUserId,
        String managerName,
        String managerEmail,
        Double centerLatitude,
        Double centerLongitude,
        boolean active,
        long totalEvents,
        long openEvents,
        LocalDateTime createdAt
) {
}
