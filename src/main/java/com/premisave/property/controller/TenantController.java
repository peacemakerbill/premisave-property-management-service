package com.premisave.property.controller;

import com.premisave.property.dto.request.TenantRegistrationRequest;
import com.premisave.property.dto.request.UpdateTenantRequest;
import com.premisave.property.dto.response.ProfileSyncStatusResponse;
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

    // One-click registration — fullName/phoneNumber/email are pulled
    // straight from the user's auth-service account (via their own JWT),
    // no form to fill in.
    @PostMapping("/quick-register")
    public ResponseEntity<TenantResponse> quickRegisterTenant(HttpServletRequest httpRequest) {
        String userId = resolveUserId(httpRequest);
        String authHeader = resolveAuthHeader(httpRequest);
        return ResponseEntity.ok(tenantService.quickRegisterTenant(userId, authHeader));
    }

    // One-click sync — overwrites fullName/phoneNumber/email to match
    // whatever's currently on file in auth-service.
    @PostMapping("/me/sync")
    public ResponseEntity<TenantResponse> syncMyTenantProfile(HttpServletRequest httpRequest) {
        String userId = resolveUserId(httpRequest);
        String authHeader = resolveAuthHeader(httpRequest);
        return ResponseEntity.ok(tenantService.syncTenantWithAuthProfile(userId, authHeader));
    }

    // Lets the frontend decide whether to show a "your profile changed —
    // sync now?" prompt, without syncing unconditionally.
    @GetMapping("/me/sync-status")
    public ResponseEntity<ProfileSyncStatusResponse> checkMyTenantSyncStatus(HttpServletRequest httpRequest) {
        String userId = resolveUserId(httpRequest);
        String authHeader = resolveAuthHeader(httpRequest);
        return ResponseEntity.ok(tenantService.checkTenantSyncStatus(userId, authHeader));
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

    // The incoming Authorization header (still present on the request —
    // JwtAuthFilter reads it but doesn't strip it) is forwarded as-is to
    // auth-service's /profile/me so auth-service authenticates the same
    // user that authenticated here.
    private String resolveAuthHeader(HttpServletRequest httpRequest) {
        String authHeader = httpRequest.getHeader("Authorization");
        if (authHeader == null || authHeader.isBlank()) {
            throw new IllegalStateException("No Authorization header found on request");
        }
        return authHeader;
    }
}