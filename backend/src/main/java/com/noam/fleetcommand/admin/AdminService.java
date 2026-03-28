package com.noam.fleetcommand.admin;

import com.noam.fleetcommand.admin.dto.AdminAssignReserveDto;
import com.noam.fleetcommand.admin.dto.AdminReserveDetailDto;
import com.noam.fleetcommand.admin.dto.AdminReserveSummaryDto;
import com.noam.fleetcommand.admin.dto.UserSummaryDto;
import com.noam.fleetcommand.common.errors.NotFoundException;
import com.noam.fleetcommand.events.Event;
import com.noam.fleetcommand.events.EventRepository;
import com.noam.fleetcommand.events.EventStatus;
import com.noam.fleetcommand.events.mapper.EventMapper;
import com.noam.fleetcommand.requests.ReserveCreationRequest;
import com.noam.fleetcommand.requests.ReserveCreationRequestRepository;
import com.noam.fleetcommand.requests.ReserveRequestStatus;
import com.noam.fleetcommand.requests.dto.ReserveCreationRequestResponseDto;
import com.noam.fleetcommand.reserves.Area;
import com.noam.fleetcommand.reserves.Reserve;
import com.noam.fleetcommand.reserves.ReserveRepository;
import com.noam.fleetcommand.reserves.dto.AreaDto;
import com.noam.fleetcommand.security.CurrentUserService;
import com.noam.fleetcommand.users.User;
import com.noam.fleetcommand.users.UserRepository;
import com.noam.fleetcommand.users.UserRole;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AdminService {

    private final CurrentUserService currentUserService;
    private final ReserveRepository reserveRepository;
    private final EventRepository eventRepository;
    private final EventMapper eventMapper;
    private final UserRepository userRepository;
    private final ReserveCreationRequestRepository reserveCreationRequestRepository;

    public AdminService(
            CurrentUserService currentUserService,
            ReserveRepository reserveRepository,
            EventRepository eventRepository,
            EventMapper eventMapper,
            UserRepository userRepository,
            ReserveCreationRequestRepository reserveCreationRequestRepository
    ) {
        this.currentUserService = currentUserService;
        this.reserveRepository = reserveRepository;
        this.eventRepository = eventRepository;
        this.eventMapper = eventMapper;
        this.userRepository = userRepository;
        this.reserveCreationRequestRepository = reserveCreationRequestRepository;
    }

    @Transactional(readOnly = true)
    public List<UserSummaryDto> getManagerUsers() {
        currentUserService.getRequiredAdmin();
        return userRepository.findByRoleOrderByNameAsc(UserRole.MANAGER).stream()
                .map(user -> new UserSummaryDto(user.getId(), user.getName(), user.getEmail(), user.getRole().name()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ReserveCreationRequestResponseDto> getReserveRequests() {
        currentUserService.getRequiredAdmin();
        return reserveCreationRequestRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toRequestResponse)
                .toList();
    }

    @Transactional
    public ReserveCreationRequestResponseDto updateReserveRequestStatus(Long requestId, ReserveRequestStatus status) {
        currentUserService.getRequiredAdmin();
        ReserveCreationRequest request = reserveCreationRequestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Reserve request not found: " + requestId));
        request.setStatus(status);
        request.setResolvedAt(status == ReserveRequestStatus.OPEN ? null : LocalDateTime.now());
        return toRequestResponse(reserveCreationRequestRepository.save(request));
    }

    @Transactional(readOnly = true)
    public List<AdminReserveSummaryDto> getReserveSummaries(String search, Long managerUserId, String region, Boolean hasOpenEvents, Boolean active) {
        currentUserService.getRequiredAdmin();

        List<Event> allEvents = eventRepository.findAll();

        return reserveRepository.findAllByOrderByNameAsc().stream()
                .map(reserve -> toReserveSummary(reserve, allEvents))
                .filter(summary -> matchesSummary(summary, search, managerUserId, region, hasOpenEvents, active))
                .toList();
    }

    @Transactional(readOnly = true)
    public AdminReserveDetailDto getReserveDetail(Long reserveId) {
        currentUserService.getRequiredAdmin();
        Reserve reserve = reserveRepository.findById(reserveId)
                .orElseThrow(() -> new NotFoundException("Reserve not found: " + reserveId));
        List<Event> reserveEvents = eventRepository.findByReserveId(reserveId);
        User manager = reserve.getManager();
        return new AdminReserveDetailDto(
                reserve.getId(),
                reserve.getName(),
                reserve.getDisplayName(),
                reserve.getRegion(),
                toAreaDto(reserve.getArea()),
                manager == null ? null : manager.getId(),
                manager == null ? null : manager.getName(),
                manager == null ? null : manager.getEmail(),
                reserve.getCenterLatitude(),
                reserve.getCenterLongitude(),
                reserve.getPolygonGeoJson(),
                manager != null,
                reserve.getCreatedAt(),
                reserveEvents.stream().map(eventMapper::toDto).toList()
        );
    }

    @Transactional
    public AdminReserveSummaryDto assignReserve(Long reserveId, AdminAssignReserveDto request) {
        currentUserService.getRequiredAdmin();
        Reserve reserve = reserveRepository.findById(reserveId)
                .orElseThrow(() -> new NotFoundException("Reserve not found: " + reserveId));

        if (request.managerUserId() == null) {
            reserve.setManager(null);
        } else {
            reserve.setManager(loadManager(request.managerUserId()));
        }

        Reserve savedReserve = reserveRepository.save(reserve);
        resolveRequestIfPresent(request.reserveRequestId(), savedReserve.getManager() == null ? ReserveRequestStatus.OPEN : ReserveRequestStatus.APPROVED);
        return toReserveSummary(savedReserve, eventRepository.findAll());
    }

    private boolean matchesSummary(AdminReserveSummaryDto summary, String search, Long managerUserId, String region, Boolean hasOpenEvents, Boolean active) {
        String managerName = summary.managerName() == null ? "" : summary.managerName();
        String managerEmail = summary.managerEmail() == null ? "" : summary.managerEmail();

        boolean matchesSearch = search == null || search.isBlank()
                || summary.name().toLowerCase().contains(search.toLowerCase())
                || summary.displayName().toLowerCase().contains(search.toLowerCase())
                || managerName.toLowerCase().contains(search.toLowerCase())
                || managerEmail.toLowerCase().contains(search.toLowerCase());
        boolean matchesManager = managerUserId == null || (summary.managerUserId() != null && summary.managerUserId().equals(managerUserId));
        boolean matchesRegion = region == null || region.isBlank() || summary.region().equalsIgnoreCase(region);
        boolean matchesOpenEvents = hasOpenEvents == null || (hasOpenEvents ? summary.openEvents() > 0 : summary.openEvents() == 0);
        boolean matchesActive = active == null || summary.active() == active;
        return matchesSearch && matchesManager && matchesRegion && matchesOpenEvents && matchesActive;
    }

    private AdminReserveSummaryDto toReserveSummary(Reserve reserve, List<Event> allEvents) {
        User manager = reserve.getManager();
        List<Event> reserveEvents = allEvents.stream()
                .filter(event -> event.getReserve().getId().equals(reserve.getId()))
                .toList();

        long openEvents = reserveEvents.stream()
                .filter(event -> event.getStatus() != EventStatus.CLOSED)
                .count();

        return new AdminReserveSummaryDto(
                reserve.getId(),
                reserve.getName(),
                reserve.getDisplayName() == null ? reserve.getName() : reserve.getDisplayName(),
                reserve.getRegion(),
                toAreaDto(reserve.getArea()),
                manager == null ? null : manager.getId(),
                manager == null ? null : manager.getName(),
                manager == null ? null : manager.getEmail(),
                reserve.getCenterLatitude(),
                reserve.getCenterLongitude(),
                manager != null,
                reserveEvents.size(),
                openEvents,
                reserve.getCreatedAt()
        );
    }

    private AreaDto toAreaDto(Area area) {
        return new AreaDto(area.getMinLatitude(), area.getMaxLatitude(), area.getMinLongitude(), area.getMaxLongitude());
    }

    private ReserveCreationRequestResponseDto toRequestResponse(ReserveCreationRequest request) {
        return new ReserveCreationRequestResponseDto(
                request.getId(),
                request.getRequestedReserveName(),
                request.getRequestMessage(),
                request.getStatus().name(),
                request.getRequestedBy().getId(),
                request.getRequestedBy().getName(),
                request.getRequestedBy().getEmail(),
                request.getCreatedAt(),
                request.getResolvedAt()
        );
    }

    private User loadManager(Long managerUserId) {
        User manager = userRepository.findById(managerUserId)
                .orElseThrow(() -> new NotFoundException("Manager user not found: " + managerUserId));
        if (manager.getRole() != UserRole.MANAGER) {
            throw new IllegalArgumentException("Reserves can only be assigned to manager users");
        }
        return manager;
    }

    private void resolveRequestIfPresent(Long reserveRequestId, ReserveRequestStatus status) {
        if (reserveRequestId == null) {
            return;
        }

        ReserveCreationRequest reserveRequest = reserveCreationRequestRepository.findById(reserveRequestId)
                .orElseThrow(() -> new NotFoundException("Reserve request not found: " + reserveRequestId));
        reserveRequest.setStatus(status);
        reserveRequest.setResolvedAt(status == ReserveRequestStatus.OPEN ? null : LocalDateTime.now());
        reserveCreationRequestRepository.save(reserveRequest);
    }

}
