package com.premisave.property.controller;

import com.premisave.property.service.OccupancyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/occupancy")
@RequiredArgsConstructor
public class OccupancyController {

    private final OccupancyService occupancyService;

    @PostMapping("/move-in")
    public ResponseEntity<String> recordMoveIn(@RequestParam String rentalUnitId,
                                               @RequestParam String tenantId,
                                               @RequestParam String leaseId) {
        occupancyService.recordMoveIn(rentalUnitId, tenantId, leaseId);
        return ResponseEntity.ok("Move-in recorded successfully");
    }
}