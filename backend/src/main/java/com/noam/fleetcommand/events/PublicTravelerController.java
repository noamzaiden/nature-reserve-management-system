package com.noam.fleetcommand.events;

import com.noam.fleetcommand.events.dto.EventResponseDto;
import com.noam.fleetcommand.events.dto.TravelerEventReportRequestDto;
import com.noam.fleetcommand.reserves.ReserveService;
import com.noam.fleetcommand.reserves.dto.ReserveResponseDto;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/public")
@Validated
public class PublicTravelerController {

    private final EventService eventService;
    private final ReserveService reserveService;
    private final TravelerMediaStorageService travelerMediaStorageService;

    public PublicTravelerController(EventService eventService,
                                    ReserveService reserveService,
                                    TravelerMediaStorageService travelerMediaStorageService) {
        this.eventService = eventService;
        this.reserveService = reserveService;
        this.travelerMediaStorageService = travelerMediaStorageService;
    }

    @GetMapping("/reserves")
    public List<ReserveResponseDto> getPublicReserves() {
        return reserveService.getPublicReserves();
    }

    @GetMapping("/events")
    public List<EventResponseDto> getPublishedEvents(@RequestParam Long reserveId) {
        return eventService.getPublishedForTravelers(reserveId);
    }

    @PostMapping(value = "/reports", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<EventResponseDto> submitTravelerReport(
            @Valid @ModelAttribute TravelerEventReportRequestDto request,
            @RequestPart(name = "attachments", required = false) List<MultipartFile> attachments
    ) {
        return ResponseEntity.ok(eventService.createTravelerReport(request, attachments));
    }

    @GetMapping("/media/{fileName:.+}")
    public ResponseEntity<Resource> getMedia(@PathVariable String fileName) {
        Resource resource = travelerMediaStorageService.loadAsResource(fileName);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }
}
