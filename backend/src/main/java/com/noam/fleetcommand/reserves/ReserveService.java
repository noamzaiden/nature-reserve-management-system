package com.noam.fleetcommand.reserves;

import com.noam.fleetcommand.common.errors.NotFoundException;
import com.noam.fleetcommand.reserves.dto.ReserveResponseDto;
import com.noam.fleetcommand.reserves.mapper.ReserveMapper;
import com.noam.fleetcommand.security.CurrentUserService;
import com.noam.fleetcommand.users.User;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ReserveService {

    private final ReserveRepository reserveRepository;
    private final ReserveMapper reserveMapper;
    private final CurrentUserService currentUserService;

    public ReserveService(
            ReserveRepository reserveRepository,
            ReserveMapper reserveMapper,
            CurrentUserService currentUserService
    ) {
        this.reserveRepository = reserveRepository;
        this.reserveMapper = reserveMapper;
        this.currentUserService = currentUserService;
    }

    public List<ReserveResponseDto> getAllReserves() {
        User currentUser = currentUserService.getRequiredManager();
        return reserveRepository.findByManagerId(currentUser.getId())
                .stream()
                .map(reserveMapper::toResponseDto)
                .toList();
    }

    public List<ReserveResponseDto> getPublicReserves() {
        return reserveRepository.findByManagerIsNotNullOrderByNameAsc().stream()
                .map(reserveMapper::toResponseDto)
                .toList();
    }

    public ReserveResponseDto getReserveById(Long id) {
        User currentUser = currentUserService.getRequiredManager();
        Reserve reserve = reserveRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Reserve not found: " + id));
        validateAccess(reserve, currentUser);

        return reserveMapper.toResponseDto(reserve);
    }

    private void validateAccess(Reserve reserve, User currentUser) {
        if (reserve.getManager() == null || !reserve.getManager().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("No access to requested reserve");
        }
    }
}
