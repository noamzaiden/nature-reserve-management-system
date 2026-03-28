package com.noam.fleetcommand.admin.dto;

public record AdminAssignReserveDto(
        Long managerUserId,
        Long reserveRequestId
) {
}
