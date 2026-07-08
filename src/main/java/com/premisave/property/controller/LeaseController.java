package com.premisave.property.controller;

import com.premisave.property.dto.request.CreateLeaseRequest;
import com.premisave.property.dto.request.UpdateLeaseRequest;
import com.premisave.property.dto.response.LeaseResponse;
import com.premisave.property.service.LeaseService;
import com.premisave.property.service.OwnerService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/leases")
@RequiredArgsConstructor
public class LeaseController {

    private final LeaseService leaseService;
    private final OwnerService ownerService;

    @PostMapping
    public ResponseEntity<LeaseResponse> createLease(@Valid @RequestBody CreateLeaseRequest request) {
        return ResponseEntity.ok(leaseService.createLease(request));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<LeaseResponse>> getAllLeases() {
        return ResponseEntity.ok(leaseService.getAllLeases());
    }

    @GetMapping("/owner")
    @PreAuthorize("hasRole('HOME_OWNER')")
    public ResponseEntity<List<LeaseResponse>> getLeasesForOwner(HttpServletRequest httpRequest) {
        String userId = (String) httpRequest.getAttribute("userId");
        String ownerId = ownerService.getOwnerByUserId(userId).getId();
        return ResponseEntity.ok(leaseService.getLeasesForOwner(ownerId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<LeaseResponse> getLease(@PathVariable String id) {
        return ResponseEntity.ok(leaseService.getLease(id));
    }

    @GetMapping("/tenant/{tenantId}")
    public ResponseEntity<List<LeaseResponse>> getLeasesByTenant(@PathVariable String tenantId) {
        return ResponseEntity.ok(leaseService.getLeasesByTenant(tenantId));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<LeaseResponse> updateLease(@PathVariable String id,
                                                       @RequestBody UpdateLeaseRequest request) {
        return ResponseEntity.ok(leaseService.updateLease(id, request));
    }

    @PostMapping("/{id}/terminate")
    public ResponseEntity<Map<String, Object>> terminateLease(@PathVariable String id) {
        LeaseResponse response = leaseService.terminateLease(id);
        return ResponseEntity.ok(Map.of(
                "message", "Lease has been terminated successfully",
                "lease", response
        ));
    }
}