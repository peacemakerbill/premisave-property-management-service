package com.premisave.property.controller;

import com.premisave.property.dto.request.TenantRegistrationRequest;
import com.premisave.property.dto.request.UpdateTenantRequest;
import com.premisave.property.dto.response.TenantResponse;
import com.premisave.property.service.TenantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tenants")
@RequiredArgsConstructor
public class TenantController {

    private final TenantService tenantService;

    @PostMapping("/register")
    public ResponseEntity<TenantResponse> registerTenant(@Valid @RequestBody TenantRegistrationRequest request) {
        TenantResponse tenant = tenantService.registerTenant(request);
        return ResponseEntity.ok(tenant);
    }

    @GetMapping("/me")
    public ResponseEntity<TenantResponse> getMyTenantProfile() {
        // userId from SecurityContext
        return ResponseEntity.ok(tenantService.getTenantByUserId("user-id-from-jwt"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TenantResponse> getTenant(@PathVariable String id) {
        return ResponseEntity.ok(tenantService.getTenantById(id));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<TenantResponse> updateTenant(@PathVariable String id,
                                                         @RequestBody UpdateTenantRequest request) {
        return ResponseEntity.ok(tenantService.updateTenant(id, request));
    }
}