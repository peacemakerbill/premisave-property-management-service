package com.premisave.property.controller;

import com.premisave.property.dto.request.MaintenanceRequest;
import com.premisave.property.dto.request.UpdateMaintenanceStatusRequest;
import com.premisave.property.dto.response.MaintenanceResponse;
import com.premisave.property.enums.MaintenanceStatus;
import com.premisave.property.security.AccessGuard;
import com.premisave.property.service.MaintenanceService;
import com.premisave.property.service.TenantService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/maintenance")
@RequiredArgsConstructor
public class MaintenanceController {

    private final MaintenanceService maintenanceService;
    private final TenantService tenantService;
    private final AccessGuard guard;

    // A tenant can only raise a request against a unit they actually occupy.
    @PostMapping
    public ResponseEntity<MaintenanceResponse> createMaintenanceRequest(@Valid @RequestBody MaintenanceRequest request,
                                                                          HttpServletRequest httpRequest) {
        String tenantId = resolveTenantId(httpRequest);
        if (request.getRentalUnitId() != null && !request.getRentalUnitId().isBlank()) {
            guard.requireUnitParticipant(request.getRentalUnitId());
        }
        MaintenanceResponse response = maintenanceService.createMaintenanceRequest(request, tenantId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<MaintenanceResponse> getMaintenanceRequest(@PathVariable String id) {
        guard.requireMaintenanceViewer(id);
        return ResponseEntity.ok(maintenanceService.getMaintenanceRequest(id));
    }

    // The tenant sees all of theirs. A landlord sees only requests on their own units.
    @GetMapping("/tenant/{tenantId}")
    public ResponseEntity<List<MaintenanceResponse>> getRequestsByTenant(@PathVariable String tenantId) {
        guard.requireTenantSelfOrLandlord(tenantId);
        List<MaintenanceResponse> requests = maintenanceService.getRequestsByTenant(tenantId);
        if (guard.seesAllOf(tenantId)) {
            return ResponseEntity.ok(requests);
        }
        Set<String> unitIds = guard.ownedUnitIds();
        return ResponseEntity.ok(requests.stream().filter(r -> unitIds.contains(r.getRentalUnitId())).toList());
    }

    @GetMapping("/unit/{rentalUnitId}")
    public ResponseEntity<List<MaintenanceResponse>> getRequestsByUnit(@PathVariable String rentalUnitId) {
        guard.requireUnitParticipant(rentalUnitId);
        return ResponseEntity.ok(maintenanceService.getRequestsByUnit(rentalUnitId));
    }

    // Admins see every request; an owner sees the requests on their own units.
    @GetMapping
    public ResponseEntity<List<MaintenanceResponse>> getRequestsByStatus(
            @RequestParam(defaultValue = "PENDING") MaintenanceStatus status) {
        if (guard.isAdmin()) {
            return ResponseEntity.ok(maintenanceService.getRequestsByStatus(status));
        }
        guard.requireOwnerId();
        Set<String> unitIds = guard.ownedUnitIds();
        return ResponseEntity.ok(maintenanceService.getRequestsByStatus(status).stream()
                .filter(r -> unitIds.contains(r.getRentalUnitId()))
                .toList());
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<MaintenanceResponse> updateStatus(@PathVariable String id,
                                                              @Valid @RequestBody UpdateMaintenanceStatusRequest request) {
        guard.requireMaintenanceManager(id);
        return ResponseEntity.ok(maintenanceService.updateStatus(id, request));
    }

    private String resolveTenantId(HttpServletRequest httpRequest) {
        String userId = (String) httpRequest.getAttribute("userId");
        return tenantService.getTenantByUserId(userId).getId();
    }
}
