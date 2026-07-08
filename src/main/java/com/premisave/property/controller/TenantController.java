package com.premisave.property.controller;

import com.premisave.property.dto.request.TenantRegistrationRequest;
import com.premisave.property.dto.request.UpdateTenantRequest;
import com.premisave.property.dto.response.TenantResponse;
import com.premisave.property.service.TenantService;
import jakarta.servlet.http.HttpServletRequest;
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
    public ResponseEntity<TenantResponse> registerTenant(@Valid @RequestBody TenantRegistrationRequest request,
                                                           HttpServletRequest httpRequest) {
        String userId = resolveUserId(httpRequest);
        TenantResponse tenant = tenantService.registerTenant(request, userId);
        return ResponseEntity.ok(tenant);
    }

    @GetMapping("/me")
    public ResponseEntity<TenantResponse> getMyTenantProfile(HttpServletRequest httpRequest) {
        String userId = resolveUserId(httpRequest);
        return ResponseEntity.ok(tenantService.getTenantByUserId(userId));
    }

    @PatchMapping("/me")
    public ResponseEntity<TenantResponse> updateMyTenantProfile(@RequestBody UpdateTenantRequest request,
                                                                  HttpServletRequest httpRequest) {
        String userId = resolveUserId(httpRequest);
        return ResponseEntity.ok(tenantService.updateTenantByUserId(request, userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TenantResponse> getTenant(@PathVariable String id) {
        return ResponseEntity.ok(tenantService.getTenantById(id));
    }

    private String resolveUserId(HttpServletRequest httpRequest) {
        String userId = (String) httpRequest.getAttribute("userId");
        if (userId == null) {
            throw new IllegalStateException("No authenticated userId found on request");
        }
        return userId;
    }
}