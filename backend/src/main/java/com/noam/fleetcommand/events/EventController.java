package com.noam.fleetcommand.events;

import com.noam.fleetcommand.events.dto.EventRequestDto;
import com.noam.fleetcommand.events.dto.EventResponseDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @GetMapping
    public ResponseEntity<List<EventResponseDto>> getEvents(@RequestParam(required = false) Long reserveId) {
        if (reserveId != null) {
            return ResponseEntity.ok(eventService.getByReserveId(reserveId));
        }
        return ResponseEntity.ok(eventService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventResponseDto> getEventById(@PathVariable Long id) {
        return ResponseEntity.ok(eventService.getById(id));
    }

    @PostMapping
    public ResponseEntity<EventResponseDto> createEvent(@Valid @RequestBody EventRequestDto request) {
        EventResponseDto created = eventService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<EventResponseDto> updateEventStatus(
            @PathVariable Long id,
            @RequestParam EventStatus status
    ) {
        return ResponseEntity.ok(eventService.updateStatus(id, status));
    }

    @PatchMapping("/{id}/priority")
    public ResponseEntity<EventResponseDto> updateEventPriority(
            @PathVariable Long id,
            @RequestParam EventPriority priority
    ) {
        return ResponseEntity.ok(eventService.updatePriority(id, priority));
    }

    @PatchMapping("/{id}/publish")
    public ResponseEntity<EventResponseDto> updatePublishState(
            @PathVariable Long id,
            @RequestParam boolean published
    ) {
        return ResponseEntity.ok(eventService.updatePublishState(id, published));
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEvent(@PathVariable Long id) {
        eventService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
