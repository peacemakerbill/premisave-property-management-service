package com.premisave.property.controller;

import com.premisave.property.dto.request.MaintenanceRequest;
import com.premisave.property.dto.response.MaintenanceResponse;
import com.premisave.property.service.MaintenanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/maintenance")
@RequiredArgsConstructor
public class MaintenanceController {

    private final MaintenanceService maintenanceService;

    @PostMapping
    public ResponseEntity<MaintenanceResponse> createMaintenanceRequest(@RequestBody MaintenanceRequest request) {
        MaintenanceResponse response = maintenanceService.createMaintenanceRequest(request, "tenant-id");
        return ResponseEntity.ok(response);
    }
}