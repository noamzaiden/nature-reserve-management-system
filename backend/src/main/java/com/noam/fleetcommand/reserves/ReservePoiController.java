package com.noam.fleetcommand.reserves;

import com.noam.fleetcommand.reserves.dto.ReservePoiRequestDto;
import com.noam.fleetcommand.reserves.dto.ReservePoiResponseDto;
import com.noam.fleetcommand.reserves.dto.ReservePoiTypeRequestDto;
import com.noam.fleetcommand.reserves.dto.ReservePoiTypeResponseDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/reserves/{reserveId:\\d+}")
public class ReservePoiController {

    private final ReservePoiService reservePoiService;

    public ReservePoiController(ReservePoiService reservePoiService) {
        this.reservePoiService = reservePoiService;
    }

    @GetMapping("/pois")
    public ResponseEntity<List<ReservePoiResponseDto>> getReservePois(@PathVariable Long reserveId) {
        return ResponseEntity.ok(reservePoiService.getReservePois(reserveId));
    }

    @PostMapping("/pois")
    public ResponseEntity<ReservePoiResponseDto> createReservePoi(
            @PathVariable Long reserveId,
            @Valid @RequestBody ReservePoiRequestDto request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reservePoiService.createReservePoi(reserveId, request));
    }

    @PutMapping("/pois/{poiId}")
    public ResponseEntity<ReservePoiResponseDto> updateReservePoi(
            @PathVariable Long reserveId,
            @PathVariable Long poiId,
            @Valid @RequestBody ReservePoiRequestDto request
    ) {
        return ResponseEntity.ok(reservePoiService.updateReservePoi(reserveId, poiId, request));
    }

    @DeleteMapping("/pois/{poiId}")
    public ResponseEntity<Void> deleteReservePoi(@PathVariable Long reserveId, @PathVariable Long poiId) {
        reservePoiService.deleteReservePoi(reserveId, poiId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/poi-types")
    public ResponseEntity<List<ReservePoiTypeResponseDto>> getReservePoiTypes(@PathVariable Long reserveId) {
        return ResponseEntity.ok(reservePoiService.getReservePoiTypes(reserveId));
    }

    @PostMapping("/poi-types")
    public ResponseEntity<ReservePoiTypeResponseDto> createReservePoiType(
            @PathVariable Long reserveId,
            @Valid @RequestBody ReservePoiTypeRequestDto request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reservePoiService.createReservePoiType(reserveId, request));
    }

    @PutMapping("/poi-types/{typeId}")
    public ResponseEntity<ReservePoiTypeResponseDto> updateReservePoiType(
            @PathVariable Long reserveId,
            @PathVariable Long typeId,
            @Valid @RequestBody ReservePoiTypeRequestDto request
    ) {
        return ResponseEntity.ok(reservePoiService.updateReservePoiType(reserveId, typeId, request));
    }

    @DeleteMapping("/poi-types/{typeId}")
    public ResponseEntity<Void> deleteReservePoiType(@PathVariable Long reserveId, @PathVariable Long typeId) {
        reservePoiService.deleteReservePoiType(reserveId, typeId);
        return ResponseEntity.noContent().build();
    }
}
