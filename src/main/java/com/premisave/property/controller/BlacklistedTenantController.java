package com.premisave.property.controller;

import com.premisave.property.dto.request.BlacklistTenantRequest;
import com.premisave.property.dto.response.BlacklistedTenantResponse;
import com.premisave.property.service.BlacklistedTenantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/blacklisted-tenants")
@RequiredArgsConstructor
public class BlacklistedTenantController {

    private final BlacklistedTenantService blacklistedTenantService;

    @PostMapping
    public ResponseEntity<BlacklistedTenantResponse> blacklistTenant(@Valid @RequestBody BlacklistTenantRequest request) {
        return ResponseEntity.ok(blacklistedTenantService.blacklistTenant(request));
    }

    @DeleteMapping("/{tenantId}")
    public ResponseEntity<Void> removeFromBlacklist(@PathVariable String tenantId) {
        blacklistedTenantService.removeFromBlacklist(tenantId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{tenantId}")
    public ResponseEntity<BlacklistedTenantResponse> getBlacklistEntry(@PathVariable String tenantId) {
        return ResponseEntity.ok(blacklistedTenantService.getBlacklistEntry(tenantId));
    }

    @GetMapping
    public ResponseEntity<List<BlacklistedTenantResponse>> getAllBlacklisted() {
        return ResponseEntity.ok(blacklistedTenantService.getAllBlacklisted());
    }
}