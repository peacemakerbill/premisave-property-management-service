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

    // ---- Lease-backed occupancy ----

    @PostMapping("/move-in/lease")
    public ResponseEntity<OccupancyResponse> recordMoveInViaLease(@RequestParam String leaseId) {
        return ResponseEntity.ok(occupancyService.recordMoveInViaLease(leaseId));
    }

    @PostMapping("/move-out/lease")
    public ResponseEntity<OccupancyResponse> recordMoveOutViaLease(@RequestParam String leaseId) {
        return ResponseEntity.ok(occupancyService.recordMoveOutViaLease(leaseId));
    }

    // ---- Direct occupancy (no lease) ----

    @PostMapping("/move-in/direct")
    public ResponseEntity<OccupancyResponse> recordDirectMoveIn(@RequestParam String rentalUnitId,
                                                                  @RequestParam String tenantId) {
        return ResponseEntity.ok(occupancyService.recordDirectMoveIn(rentalUnitId, tenantId));
    }

    @PostMapping("/move-out/direct")
    public ResponseEntity<OccupancyResponse> recordDirectMoveOut(@RequestParam String rentalUnitId,
                                                                   @RequestParam String tenantId) {
        return ResponseEntity.ok(occupancyService.recordDirectMoveOut(rentalUnitId, tenantId));
    }

    // ---- Read endpoints ----

    @GetMapping("/unit/{rentalUnitId}")
    public ResponseEntity<List<OccupancyResponse>> getUnitHistory(@PathVariable String rentalUnitId) {
        return ResponseEntity.ok(occupancyService.getUnitHistory(rentalUnitId));
    }

    @GetMapping("/unit/{rentalUnitId}/current")
    public ResponseEntity<OccupancyResponse> getCurrentOccupancyForUnit(@PathVariable String rentalUnitId) {
        return ResponseEntity.ok(occupancyService.getCurrentOccupancyForUnit(rentalUnitId));
    }

    @GetMapping("/property/{propertyId}")
    public ResponseEntity<List<OccupancyResponse>> getPropertyHistory(@PathVariable String propertyId) {
        return ResponseEntity.ok(occupancyService.getPropertyHistory(propertyId));
    }

    @GetMapping("/property/{propertyId}/current")
    public ResponseEntity<OccupancyResponse> getCurrentOccupancyForProperty(@PathVariable String propertyId) {
        return ResponseEntity.ok(occupancyService.getCurrentOccupancyForProperty(propertyId));
    }

    @GetMapping("/tenant/{tenantId}")
    public ResponseEntity<List<OccupancyResponse>> getTenantHistory(@PathVariable String tenantId) {
        return ResponseEntity.ok(occupancyService.getTenantHistory(tenantId));
    }
}