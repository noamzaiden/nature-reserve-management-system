package com.noam.fleetcommand.reserves;

import com.noam.fleetcommand.reserves.dto.ReserveResponseDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/reserves")
public class ReserveController {

    private final ReserveService reserveService;

    public ReserveController(ReserveService reserveService) {
        this.reserveService = reserveService;
    }

    @GetMapping
    public List<ReserveResponseDto> getAllReserves() {
        return reserveService.getAllReserves();
    }

    @GetMapping("/{id:\\d+}")
    public ResponseEntity<ReserveResponseDto> getReserveById(@PathVariable Long id) {
        return ResponseEntity.ok(reserveService.getReserveById(id));
    }

}
