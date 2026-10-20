package com.premisave.property.controller;

import com.premisave.property.dto.request.BlacklistTenantRequest;
import com.premisave.property.dto.response.BlacklistedTenantResponse;
import com.premisave.property.security.AccessGuard;
import com.premisave.property.service.BlacklistedTenantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/blacklisted-tenants")
@RequiredArgsConstructor
public class BlacklistedTenantController {

    private final BlacklistedTenantService blacklistedTenantService;
    private final AccessGuard guard;

    // An owner can blacklist a tenant who rents (or rented) from them. Admins can blacklist anyone.
    @PostMapping
    public ResponseEntity<BlacklistedTenantResponse> blacklistTenant(@Valid @RequestBody BlacklistTenantRequest request) {
        guard.requireTenantLandlord(request.getTenantId());
        String ownerId = guard.isAdmin() ? null : guard.requireOwnerId();
        return ResponseEntity.ok(blacklistedTenantService.blacklistTenant(request, ownerId));
    }

    @DeleteMapping("/{tenantId}")
    public ResponseEntity<Map<String, String>> removeFromBlacklist(@PathVariable String tenantId) {
        boolean admin = guard.isAdmin();
        String ownerId = admin ? null : guard.requireOwnerId();
        blacklistedTenantService.removeFromBlacklist(tenantId, ownerId, admin);
        return ResponseEntity.ok(Map.of("message", "Tenant has been removed from the blacklist successfully"));
    }

    // Owners screen prospective tenants against the list, so any registered owner can look an entry up.
    // The tenant can see their own entry. Nobody else can.
    @GetMapping("/{tenantId}")
    public ResponseEntity<BlacklistedTenantResponse> getBlacklistEntry(@PathVariable String tenantId) {
        if (!guard.isAdmin() && !guard.isSelf(tenantId)) {
            guard.requireOwnerId();
        }
        return ResponseEntity.ok(blacklistedTenantService.getBlacklistEntry(tenantId));
    }

    @GetMapping
    public ResponseEntity<List<BlacklistedTenantResponse>> getAllBlacklisted() {
        boolean admin = guard.isAdmin();
        String ownerId = admin ? null : guard.requireOwnerId();
        return ResponseEntity.ok(blacklistedTenantService.getAllBlacklisted(ownerId, admin));
    }
}
