package com.noam.fleetcommand.requests;

import com.noam.fleetcommand.requests.dto.ReserveCreationRequestCreateDto;
import com.noam.fleetcommand.requests.dto.ReserveCreationRequestResponseDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reserve-requests")
public class ReserveCreationRequestController {

    private final ReserveCreationRequestService reserveCreationRequestService;

    public ReserveCreationRequestController(ReserveCreationRequestService reserveCreationRequestService) {
        this.reserveCreationRequestService = reserveCreationRequestService;
    }

    @PostMapping
    public ResponseEntity<ReserveCreationRequestResponseDto> create(@Valid @RequestBody ReserveCreationRequestCreateDto request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reserveCreationRequestService.create(request));
    }

    @GetMapping("/mine")
    public List<ReserveCreationRequestResponseDto> getMine() {
        return reserveCreationRequestService.getMine();
    }
}
