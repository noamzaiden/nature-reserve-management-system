package com.noam.fleetcommand.events;

import com.noam.fleetcommand.common.errors.NotFoundException;
import com.noam.fleetcommand.events.dto.EventRequestDto;
import com.noam.fleetcommand.events.dto.EventResponseDto;
import com.noam.fleetcommand.events.dto.TravelerEventReportRequestDto;
import com.noam.fleetcommand.events.mapper.EventMapper;
import com.noam.fleetcommand.reserves.Reserve;
import com.noam.fleetcommand.reserves.ReserveRepository;
import com.noam.fleetcommand.security.CurrentUserService;
import com.noam.fleetcommand.users.User;
import com.noam.fleetcommand.users.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class EventService {

    private final EventRepository eventRepository;
    private final ReserveRepository reserveRepository;
    private final UserRepository userRepository;
    private final EventMapper eventMapper;
    private final EventLogRepository eventLogRepository;
    private final CurrentUserService currentUserService;
    private final TravelerMediaStorageService travelerMediaStorageService;

    public EventService(EventRepository eventRepository, ReserveRepository reserveRepository, UserRepository userRepository,
                        EventMapper eventMapper, EventLogRepository eventLogRepository,
                        CurrentUserService currentUserService,
                        TravelerMediaStorageService travelerMediaStorageService) {
        this.eventRepository = eventRepository;
        this.reserveRepository = reserveRepository;
        this.userRepository = userRepository;
        this.eventMapper = eventMapper;
        this.eventLogRepository = eventLogRepository;
        this.currentUserService = currentUserService;
        this.travelerMediaStorageService = travelerMediaStorageService;
    }

    @Transactional
    public EventResponseDto create(EventRequestDto requestDto) {
        Reserve reserve = reserveRepository.findById(requestDto.getReserveId())
                .orElseThrow(() -> new NotFoundException("Reserve not found with id: " + requestDto.getReserveId()));

        validateReserveAccess(reserve.getId());
        validateCoordinatesInsideReserve(reserve, requestDto.getLatitude(), requestDto.getLongitude());

        Event event = eventMapper.toEntity(requestDto);
        event.setReserve(reserve);
        event.setOrigin(EventOrigin.MANAGER);
        event.setPublishedToTravelers(Boolean.TRUE.equals(requestDto.getPublishedToTravelers()));

        if (requestDto.getAssignedUserId() != null) {
            User assignedUser = userRepository.findById(requestDto.getAssignedUserId())
                    .orElseThrow(() -> new NotFoundException("User not found with id: " + requestDto.getAssignedUserId()));
            event.setAssignedUser(assignedUser);
        }

        Event savedEvent = eventRepository.save(event);
        createLog(savedEvent, "EVENT_CREATED", requestDto.getDescription(), currentUserEntity());
        return eventMapper.toDto(savedEvent);
    }

    public EventResponseDto getById(Long id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Event not found with id: " + id));
        validateReserveAccess(event.getReserve().getId());
        return eventMapper.toDto(event);
    }

    @Transactional(readOnly = true)
    public List<EventResponseDto> getAll() {
        User currentUser = currentUserService.getRequiredUser();
        return eventRepository.findByReserveManagerId(currentUser.getId()).stream()
                .map(eventMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<EventResponseDto> getByReserveId(Long reserveId) {
        validateReserveAccess(reserveId);
        return eventRepository.findByReserveId(reserveId).stream()
                .map(eventMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<EventResponseDto> getPublishedForTravelers(Long reserveId) {
        Reserve reserve = reserveRepository.findById(reserveId)
                .orElseThrow(() -> new NotFoundException("Reserve not found with id: " + reserveId));
        if (reserve.getManager() == null) {
            return List.of();
        }
        return eventRepository.findByReserveIdAndPublishedToTravelersTrueAndStatusNot(reserveId, EventStatus.CLOSED).stream()
                .map(eventMapper::toDto)
                .toList();
    }

    @Transactional
    public EventResponseDto createTravelerReport(TravelerEventReportRequestDto requestDto, List<MultipartFile> attachments) {
        Reserve reserve = reserveRepository.findById(requestDto.getReserveId())
                .orElseThrow(() -> new NotFoundException("Reserve not found with id: " + requestDto.getReserveId()));
        if (reserve.getManager() == null) {
            throw new IllegalArgumentException("Traveler reports can only be submitted for active reserves");
        }

        Double latitude = requestDto.getLatitude();
        Double longitude = requestDto.getLongitude();
        if (latitude == null || longitude == null) {
            latitude = (reserve.getArea().getMinLatitude() + reserve.getArea().getMaxLatitude()) / 2;
            longitude = (reserve.getArea().getMinLongitude() + reserve.getArea().getMaxLongitude()) / 2;
        } else {
            validateCoordinatesInsideReserve(reserve, latitude, longitude);
        }

        Event event = new Event();
        event.setReserve(reserve);
        event.setPriority(EventPriority.LOW);
        event.setStatus(EventStatus.OPEN);
        event.setType(requestDto.getType());
        event.setOrigin(EventOrigin.TRAVELER_REPORT);
        event.setDescription(requestDto.getDescription());
        event.setReporterName(requestDto.getReporterName());
        event.setPublishedToTravelers(false);
        event.setLatitude(latitude);
        event.setLongitude(longitude);

        for (EventMedia mediaItem : travelerMediaStorageService.store(attachments)) {
            event.addMedia(mediaItem);
        }

        Event savedEvent = eventRepository.save(event);
        createLog(savedEvent, "TRAVELER_REPORT_CREATED", requestDto.getDescription(), null);
        return eventMapper.toDto(savedEvent);
    }

    @Transactional
    public EventResponseDto updateStatus(Long id, EventStatus status) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Event not found with id: " + id));
        validateReserveAccess(event.getReserve().getId());
        event.setStatus(status);
        if (status == EventStatus.CLOSED) {
            event.setClosedAt(LocalDateTime.now());
        } else {
            event.setClosedAt(null);
        }
        Event saved = eventRepository.save(event);
        createLog(saved, "STATUS_UPDATED", status.name(), currentUserEntity());
        return eventMapper.toDto(saved);
    }

    @Transactional
    public EventResponseDto updatePriority(Long id, EventPriority priority) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Event not found with id: " + id));
        validateReserveAccess(event.getReserve().getId());
        event.setPriority(priority);
        Event saved = eventRepository.save(event);
        createLog(saved, "PRIORITY_UPDATED", priority.name(), currentUserEntity());
        return eventMapper.toDto(saved);
    }

    @Transactional
    public EventResponseDto updatePublishState(Long id, boolean published) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Event not found with id: " + id));
        validateReserveAccess(event.getReserve().getId());
        event.setPublishedToTravelers(published);
        Event saved = eventRepository.save(event);
        createLog(saved, "PUBLISH_STATE_UPDATED", Boolean.toString(published), currentUserEntity());
        return eventMapper.toDto(saved);
    }

    @Transactional
    public void delete(Long id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Event not found with id: " + id));
        validateReserveAccess(event.getReserve().getId());
        eventRepository.deleteById(id);
    }

    private void validateCoordinatesInsideReserve(Reserve reserve, Double lat, Double lon) {
        if (!reserve.getArea().contains(lat, lon)) {
            throw new IllegalArgumentException("Event location is outside reserve boundaries");
        }
    }

    private void createLog(Event event, String action, String note, User actor) {
        eventLogRepository.save(new EventLog(event, action, note, actor));
    }

    private User currentUserEntity() {
        return currentUserService.getRequiredUser();
    }

    private void validateReserveAccess(Long reserveId) {
        User currentUser = currentUserService.getRequiredUser();
        Reserve reserve = reserveRepository.findById(reserveId)
                .orElseThrow(() -> new NotFoundException("Reserve not found with id: " + reserveId));
        if (reserve.getManager() == null || !reserve.getManager().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("No access to requested reserve");
        }
    }
}
