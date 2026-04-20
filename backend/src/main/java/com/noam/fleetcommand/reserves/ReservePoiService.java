package com.noam.fleetcommand.reserves;

import com.noam.fleetcommand.common.errors.NotFoundException;
import com.noam.fleetcommand.reserves.dto.ReservePoiRequestDto;
import com.noam.fleetcommand.reserves.dto.ReservePoiResponseDto;
import com.noam.fleetcommand.reserves.dto.ReservePoiTypeRequestDto;
import com.noam.fleetcommand.reserves.dto.ReservePoiTypeResponseDto;
import com.noam.fleetcommand.security.CurrentUserService;
import com.noam.fleetcommand.users.User;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ReservePoiService {

    private static final List<String> DEFAULT_TYPE_NAMES = List.of(
            "Entrance",
            "Exit",
            "Toilet",
            "Information Desk",
            "Parking",
            "Water Point",
            "Picnic Area",
            "Viewpoint",
            "First Aid",
            "Ranger Station"
    );

    private final ReserveRepository reserveRepository;
    private final ReservePoiRepository reservePoiRepository;
    private final ReservePoiTypeRepository reservePoiTypeRepository;
    private final CurrentUserService currentUserService;

    public ReservePoiService(ReserveRepository reserveRepository,
                             ReservePoiRepository reservePoiRepository,
                             ReservePoiTypeRepository reservePoiTypeRepository,
                             CurrentUserService currentUserService) {
        this.reserveRepository = reserveRepository;
        this.reservePoiRepository = reservePoiRepository;
        this.reservePoiTypeRepository = reservePoiTypeRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    public List<ReservePoiResponseDto> getReservePois(Long reserveId) {
        Reserve reserve = getManagedReserve(reserveId);
        return reservePoiRepository.findByReserveIdOrderByNameAsc(reserve.getId()).stream()
                .map(this::toPoiResponse)
                .toList();
    }

    @Transactional
    public ReservePoiResponseDto createReservePoi(Long reserveId, ReservePoiRequestDto request) {
        Reserve reserve = getManagedReserve(reserveId);
        ensureDefaultTypes(reserve);
        ReservePoiType type = getReservePoiType(reserve, request.getTypeId());
        validatePoiCoordinates(reserve, request.getLatitude(), request.getLongitude());

        ReservePoi poi = new ReservePoi(
                reserve,
                type,
                request.getName().trim(),
                normalizeText(request.getDescription()),
                request.getLatitude(),
                request.getLongitude()
        );

        return toPoiResponse(reservePoiRepository.save(poi));
    }

    @Transactional
    public ReservePoiResponseDto updateReservePoi(Long reserveId, Long poiId, ReservePoiRequestDto request) {
        Reserve reserve = getManagedReserve(reserveId);
        ensureDefaultTypes(reserve);
        ReservePoi poi = reservePoiRepository.findByIdAndReserveId(poiId, reserveId)
                .orElseThrow(() -> new NotFoundException("Reserve POI not found: " + poiId));
        ReservePoiType type = getReservePoiType(reserve, request.getTypeId());
        validatePoiCoordinates(reserve, request.getLatitude(), request.getLongitude());

        poi.setType(type);
        poi.setName(request.getName().trim());
        poi.setDescription(normalizeText(request.getDescription()));
        poi.setLatitude(request.getLatitude());
        poi.setLongitude(request.getLongitude());

        return toPoiResponse(reservePoiRepository.save(poi));
    }

    @Transactional
    public void deleteReservePoi(Long reserveId, Long poiId) {
        getManagedReserve(reserveId);
        ReservePoi poi = reservePoiRepository.findByIdAndReserveId(poiId, reserveId)
                .orElseThrow(() -> new NotFoundException("Reserve POI not found: " + poiId));
        reservePoiRepository.delete(poi);
    }

    @Transactional
    public List<ReservePoiTypeResponseDto> getReservePoiTypes(Long reserveId) {
        Reserve reserve = getManagedReserve(reserveId);
        ensureDefaultTypes(reserve);
        return reservePoiTypeRepository.findByReserveIdOrderBySystemDefaultDescNameAsc(reserveId).stream()
                .map(this::toPoiTypeResponse)
                .toList();
    }

    @Transactional
    public ReservePoiTypeResponseDto createReservePoiType(Long reserveId, ReservePoiTypeRequestDto request) {
        Reserve reserve = getManagedReserve(reserveId);
        ensureDefaultTypes(reserve);
        String normalizedName = request.getName().trim();
        validateUniqueTypeName(reserveId, normalizedName, null);

        ReservePoiType type = new ReservePoiType(reserve, normalizedName, false);
        return toPoiTypeResponse(reservePoiTypeRepository.save(type));
    }

    @Transactional
    public ReservePoiTypeResponseDto updateReservePoiType(Long reserveId, Long typeId, ReservePoiTypeRequestDto request) {
        Reserve reserve = getManagedReserve(reserveId);
        ensureDefaultTypes(reserve);
        ReservePoiType type = getReservePoiType(reserve, typeId);
        String normalizedName = request.getName().trim();
        validateUniqueTypeName(reserveId, normalizedName, type.getId());

        type.setName(normalizedName);
        return toPoiTypeResponse(reservePoiTypeRepository.save(type));
    }

    @Transactional
    public void deleteReservePoiType(Long reserveId, Long typeId) {
        Reserve reserve = getManagedReserve(reserveId);
        ensureDefaultTypes(reserve);
        ReservePoiType type = getReservePoiType(reserve, typeId);

        if (type.isSystemDefault()) {
            throw new IllegalArgumentException("Default POI types cannot be deleted");
        }
        if (reservePoiRepository.existsByTypeId(typeId)) {
            throw new IllegalArgumentException("Cannot delete a POI type that is still in use");
        }

        reservePoiTypeRepository.delete(type);
    }

    @Transactional(readOnly = true)
    public List<ReservePoiResponseDto> getPublicReservePois(Long reserveId) {
        Reserve reserve = reserveRepository.findById(reserveId)
                .orElseThrow(() -> new NotFoundException("Reserve not found: " + reserveId));
        if (reserve.getManager() == null) {
            return List.of();
        }
        return reservePoiRepository.findByReserveIdOrderByNameAsc(reserveId).stream()
                .map(this::toPoiResponse)
                .toList();
    }

    private Reserve getManagedReserve(Long reserveId) {
        User currentUser = currentUserService.getRequiredManager();
        Reserve reserve = reserveRepository.findById(reserveId)
                .orElseThrow(() -> new NotFoundException("Reserve not found: " + reserveId));
        if (reserve.getManager() == null || !reserve.getManager().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("No access to requested reserve");
        }
        return reserve;
    }

    private ReservePoiType getReservePoiType(Reserve reserve, Long typeId) {
        return reservePoiTypeRepository.findByIdAndReserveId(typeId, reserve.getId())
                .orElseThrow(() -> new NotFoundException("Reserve POI type not found: " + typeId));
    }

    private void validatePoiCoordinates(Reserve reserve, Double latitude, Double longitude) {
        if (!reserve.getArea().contains(latitude, longitude)) {
            throw new IllegalArgumentException("POI location is outside reserve boundaries");
        }
    }

    private void validateUniqueTypeName(Long reserveId, String normalizedName, Long currentTypeId) {
        reservePoiTypeRepository.findByReserveIdAndNameIgnoreCase(reserveId, normalizedName)
                .ifPresent(existing -> {
                    if (!existing.getId().equals(currentTypeId)) {
                        throw new IllegalArgumentException("A POI type with this name already exists for the reserve");
                    }
                });
    }

    private void ensureDefaultTypes(Reserve reserve) {
        if (reservePoiTypeRepository.existsByReserveId(reserve.getId())) {
            return;
        }

        List<ReservePoiType> defaultTypes = DEFAULT_TYPE_NAMES.stream()
                .map(name -> new ReservePoiType(reserve, name, true))
                .toList();
        reservePoiTypeRepository.saveAll(defaultTypes);
    }

    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private ReservePoiResponseDto toPoiResponse(ReservePoi poi) {
        return new ReservePoiResponseDto(
                poi.getId(),
                poi.getReserve().getId(),
                poi.getType().getId(),
                poi.getType().getName(),
                poi.getName(),
                poi.getDescription(),
                poi.getLatitude(),
                poi.getLongitude(),
                poi.getCreatedAt()
        );
    }

    private ReservePoiTypeResponseDto toPoiTypeResponse(ReservePoiType type) {
        return new ReservePoiTypeResponseDto(
                type.getId(),
                type.getReserve().getId(),
                type.getName(),
                type.isSystemDefault(),
                type.getCreatedAt()
        );
    }
}
