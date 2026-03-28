package com.noam.fleetcommand.admin;

import com.noam.fleetcommand.admin.dto.AdminAssignReserveDto;
import com.noam.fleetcommand.admin.dto.AdminReserveDetailDto;
import com.noam.fleetcommand.admin.dto.AdminReserveSummaryDto;
import com.noam.fleetcommand.admin.dto.UserSummaryDto;
import com.noam.fleetcommand.requests.ReserveRequestStatus;
import com.noam.fleetcommand.requests.dto.ReserveCreationRequestResponseDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/users")
    public List<UserSummaryDto> getManagerUsers() {
        return adminService.getManagerUsers();
    }

    @GetMapping("/reserve-requests")
    public List<ReserveCreationRequestResponseDto> getReserveRequests() {
        return adminService.getReserveRequests();
    }

    @PatchMapping("/reserve-requests/{id}")
    public ReserveCreationRequestResponseDto updateReserveRequestStatus(
            @PathVariable Long id,
            @RequestParam ReserveRequestStatus status
    ) {
        return adminService.updateReserveRequestStatus(id, status);
    }

    @GetMapping("/reserves")
    public List<AdminReserveSummaryDto> getReserveSummaries(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long managerUserId,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) Boolean hasOpenEvents,
            @RequestParam(required = false) Boolean active
    ) {
        return adminService.getReserveSummaries(search, managerUserId, region, hasOpenEvents, active);
    }

    @GetMapping("/reserves/{id}")
    public AdminReserveDetailDto getReserveDetail(@PathVariable Long id) {
        return adminService.getReserveDetail(id);
    }

    @PatchMapping("/reserves/{id}/assignment")
    public AdminReserveSummaryDto updateReserveAssignment(@PathVariable Long id, @RequestBody AdminAssignReserveDto request) {
        return adminService.assignReserve(id, request);
    }
}
