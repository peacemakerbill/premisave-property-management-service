package com.premisave.property.controller;

import com.premisave.property.dto.response.OccupancyResponse;
import com.premisave.property.security.AccessGuard;
import com.premisave.property.service.OccupancyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

// Move-ins, move-outs and occupancy history are the landlord's records:
// they belong to whoever manages the property.
@RestController
@RequestMapping("/api/v1/occupancy")
@RequiredArgsConstructor
public class OccupancyController {

    private final OccupancyService occupancyService;
    private final AccessGuard guard;

    // ---- Lease-backed occupancy ----

    @PostMapping("/move-in/lease")
    public ResponseEntity<OccupancyResponse> recordMoveInViaLease(@RequestParam String leaseId) {
        guard.requireLeaseManager(leaseId);
        return ResponseEntity.ok(occupancyService.recordMoveInViaLease(leaseId));
    }

    @PostMapping("/move-out/lease")
    public ResponseEntity<OccupancyResponse> recordMoveOutViaLease(@RequestParam String leaseId) {
        guard.requireLeaseManager(leaseId);
        return ResponseEntity.ok(occupancyService.recordMoveOutViaLease(leaseId));
    }

    // ---- Direct occupancy (no lease) ----

    @PostMapping("/move-in/direct")
    public ResponseEntity<OccupancyResponse> recordDirectMoveIn(@RequestParam String rentalUnitId,
                                                                  @RequestParam String tenantId) {
        guard.requireUnitManager(rentalUnitId);
        return ResponseEntity.ok(occupancyService.recordDirectMoveIn(rentalUnitId, tenantId));
    }

    @PostMapping("/move-out/direct")
    public ResponseEntity<OccupancyResponse> recordDirectMoveOut(@RequestParam String rentalUnitId,
                                                                   @RequestParam String tenantId) {
        guard.requireUnitManager(rentalUnitId);
        return ResponseEntity.ok(occupancyService.recordDirectMoveOut(rentalUnitId, tenantId));
    }

    // ---- Read endpoints ----

    @GetMapping("/unit/{rentalUnitId}")
    public ResponseEntity<List<OccupancyResponse>> getUnitHistory(@PathVariable String rentalUnitId) {
        guard.requireUnitManager(rentalUnitId);
        return ResponseEntity.ok(occupancyService.getUnitHistory(rentalUnitId));
    }

    @GetMapping("/unit/{rentalUnitId}/current")
    public ResponseEntity<OccupancyResponse> getCurrentOccupancyForUnit(@PathVariable String rentalUnitId) {
        guard.requireUnitManager(rentalUnitId);
        return ResponseEntity.ok(occupancyService.getCurrentOccupancyForUnit(rentalUnitId));
    }

    @GetMapping("/property/{propertyId}")
    public ResponseEntity<List<OccupancyResponse>> getPropertyHistory(@PathVariable String propertyId) {
        guard.requirePropertyManager(propertyId);
        return ResponseEntity.ok(occupancyService.getPropertyHistory(propertyId));
    }

    @GetMapping("/property/{propertyId}/current")
    public ResponseEntity<OccupancyResponse> getCurrentOccupancyForProperty(@PathVariable String propertyId) {
        guard.requirePropertyManager(propertyId);
        return ResponseEntity.ok(occupancyService.getCurrentOccupancyForProperty(propertyId));
    }

    // The tenant sees their whole history. A landlord sees only stays at their own properties.
    @GetMapping("/tenant/{tenantId}")
    public ResponseEntity<List<OccupancyResponse>> getTenantHistory(@PathVariable String tenantId) {
        guard.requireTenantSelfOrLandlord(tenantId);
        List<OccupancyResponse> history = occupancyService.getTenantHistory(tenantId);
        if (guard.seesAllOf(tenantId)) {
            return ResponseEntity.ok(history);
        }
        Set<String> propertyIds = guard.ownedPropertyIds();
        return ResponseEntity.ok(history.stream().filter(o -> propertyIds.contains(o.getPropertyId())).toList());
    }
}
