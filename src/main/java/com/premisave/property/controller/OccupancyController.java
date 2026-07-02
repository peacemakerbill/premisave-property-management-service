package com.premisave.property.controller;

import com.premisave.property.dto.response.OccupancyResponse;
import com.premisave.property.service.OccupancyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/occupancy")
@RequiredArgsConstructor
public class OccupancyController {

    private final OccupancyService occupancyService;

    @PostMapping("/move-in")
    public ResponseEntity<OccupancyResponse> recordMoveIn(@RequestParam String rentalUnitId,
                                                            @RequestParam String tenantId,
                                                            @RequestParam String leaseId) {
        return ResponseEntity.ok(occupancyService.recordMoveIn(rentalUnitId, tenantId, leaseId));
    }

    @PostMapping("/move-out")
    public ResponseEntity<OccupancyResponse> recordMoveOut(@RequestParam String rentalUnitId,
                                                             @RequestParam String tenantId) {
        return ResponseEntity.ok(occupancyService.recordMoveOut(rentalUnitId, tenantId));
    }

    @GetMapping("/unit/{rentalUnitId}")
    public ResponseEntity<List<OccupancyResponse>> getUnitHistory(@PathVariable String rentalUnitId) {
        return ResponseEntity.ok(occupancyService.getUnitHistory(rentalUnitId));
    }

    @GetMapping("/unit/{rentalUnitId}/current")
    public ResponseEntity<OccupancyResponse> getCurrentOccupancy(@PathVariable String rentalUnitId) {
        return ResponseEntity.ok(occupancyService.getCurrentOccupancy(rentalUnitId));
    }

    @GetMapping("/tenant/{tenantId}")
    public ResponseEntity<List<OccupancyResponse>> getTenantHistory(@PathVariable String tenantId) {
        return ResponseEntity.ok(occupancyService.getTenantHistory(tenantId));
    }
}