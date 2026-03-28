package com.noam.fleetcommand.admin.dto;

import com.noam.fleetcommand.events.dto.EventResponseDto;
import com.noam.fleetcommand.reserves.dto.AreaDto;

import java.time.LocalDateTime;
import java.util.List;

public record AdminReserveDetailDto(
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
        String polygonGeoJson,
        boolean active,
        LocalDateTime createdAt,
        List<EventResponseDto> events
) {
}
