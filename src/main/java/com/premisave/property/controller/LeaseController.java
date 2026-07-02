package com.premisave.property.controller;

import com.premisave.property.dto.request.CreateLeaseRequest;
import com.premisave.property.dto.request.UpdateLeaseRequest;
import com.premisave.property.dto.response.LeaseResponse;
import com.premisave.property.service.LeaseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/leases")
@RequiredArgsConstructor
public class LeaseController {

    private final LeaseService leaseService;

    @PostMapping
    public ResponseEntity<LeaseResponse> createLease(@Valid @RequestBody CreateLeaseRequest request) {
        return ResponseEntity.ok(leaseService.createLease(request));
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
    public ResponseEntity<LeaseResponse> terminateLease(@PathVariable String id) {
        return ResponseEntity.ok(leaseService.terminateLease(id));
    }
}