package com.premisave.property.controller;

import com.premisave.property.dto.request.CreateLeaseRequest;
import com.premisave.property.dto.response.LeaseResponse;
import com.premisave.property.service.LeaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/leases")
@RequiredArgsConstructor
public class LeaseController {

    private final LeaseService leaseService;

    @PostMapping
    public ResponseEntity<LeaseResponse> createLease(@RequestBody CreateLeaseRequest request) {
        LeaseResponse lease = leaseService.createLease(request);
        return ResponseEntity.ok(lease);
    }
}