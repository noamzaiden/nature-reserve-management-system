package com.noam.fleetcommand.requests;

import com.noam.fleetcommand.requests.dto.ReserveCreationRequestCreateDto;
import com.noam.fleetcommand.requests.dto.ReserveCreationRequestResponseDto;
import com.noam.fleetcommand.security.CurrentUserService;
import com.noam.fleetcommand.users.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ReserveCreationRequestService {

    private final ReserveCreationRequestRepository reserveCreationRequestRepository;
    private final CurrentUserService currentUserService;

    public ReserveCreationRequestService(
            ReserveCreationRequestRepository reserveCreationRequestRepository,
            CurrentUserService currentUserService
    ) {
        this.reserveCreationRequestRepository = reserveCreationRequestRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional
    public ReserveCreationRequestResponseDto create(ReserveCreationRequestCreateDto request) {
        User currentUser = currentUserService.getRequiredManager();

        ReserveCreationRequest entity = new ReserveCreationRequest();
        entity.setRequestedReserveName(request.getReserveName().trim());
        entity.setRequestMessage(request.getMessage().trim());
        entity.setRequestedBy(currentUser);

        return toResponse(reserveCreationRequestRepository.save(entity));
    }

    @Transactional(readOnly = true)
    public List<ReserveCreationRequestResponseDto> getMine() {
        User currentUser = currentUserService.getRequiredManager();
        return reserveCreationRequestRepository.findByRequestedByIdOrderByCreatedAtDesc(currentUser.getId()).stream()
                .map(this::toResponse)
                .toList();
    }

    private ReserveCreationRequestResponseDto toResponse(ReserveCreationRequest request) {
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
}
