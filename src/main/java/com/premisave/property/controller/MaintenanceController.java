package com.premisave.property.controller;

import com.premisave.property.dto.request.MaintenanceRequest;
import com.premisave.property.dto.request.UpdateMaintenanceStatusRequest;
import com.premisave.property.dto.response.MaintenanceResponse;
import com.premisave.property.enums.MaintenanceStatus;
import com.premisave.property.service.MaintenanceService;
import com.premisave.property.service.TenantService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/maintenance")
@RequiredArgsConstructor
public class MaintenanceController {

    private final MaintenanceService maintenanceService;
    private final TenantService tenantService;

    @PostMapping
    public ResponseEntity<MaintenanceResponse> createMaintenanceRequest(@Valid @RequestBody MaintenanceRequest request,
                                                                          HttpServletRequest httpRequest) {
        String tenantId = resolveTenantId(httpRequest);
        MaintenanceResponse response = maintenanceService.createMaintenanceRequest(request, tenantId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<MaintenanceResponse> getMaintenanceRequest(@PathVariable String id) {
        return ResponseEntity.ok(maintenanceService.getMaintenanceRequest(id));
    }

    @GetMapping("/tenant/{tenantId}")
    public ResponseEntity<List<MaintenanceResponse>> getRequestsByTenant(@PathVariable String tenantId) {
        return ResponseEntity.ok(maintenanceService.getRequestsByTenant(tenantId));
    }

    @GetMapping("/unit/{rentalUnitId}")
    public ResponseEntity<List<MaintenanceResponse>> getRequestsByUnit(@PathVariable String rentalUnitId) {
        return ResponseEntity.ok(maintenanceService.getRequestsByUnit(rentalUnitId));
    }

    @GetMapping
    public ResponseEntity<List<MaintenanceResponse>> getRequestsByStatus(
            @RequestParam(defaultValue = "PENDING") MaintenanceStatus status) {
        return ResponseEntity.ok(maintenanceService.getRequestsByStatus(status));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<MaintenanceResponse> updateStatus(@PathVariable String id,
                                                              @Valid @RequestBody UpdateMaintenanceStatusRequest request) {
        return ResponseEntity.ok(maintenanceService.updateStatus(id, request));
    }

    private String resolveTenantId(HttpServletRequest httpRequest) {
        String userId = (String) httpRequest.getAttribute("userId");
        return tenantService.getTenantByUserId(userId).getId();
    }
}