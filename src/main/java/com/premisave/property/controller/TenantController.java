package com.premisave.property.controller;

import com.premisave.property.dto.request.TenantRegistrationRequest;
import com.premisave.property.dto.response.TenantResponse;
import com.premisave.property.service.TenantService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tenants")
@RequiredArgsConstructor
public class TenantController {

    private final TenantService tenantService;

    @PostMapping("/register")
    public ResponseEntity<TenantResponse> registerTenant(@RequestBody TenantRegistrationRequest request) {
        TenantResponse tenant = tenantService.registerTenant(request);
        return ResponseEntity.ok(tenant);
    }
}