package com.premisave.property.controller;

import com.premisave.property.dto.request.RefundDepositRequest;
import com.premisave.property.dto.request.SecurityDepositRequest;
import com.premisave.property.dto.response.SecurityDepositResponse;
import com.premisave.property.service.SecurityDepositService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/security-deposits")
@RequiredArgsConstructor
public class SecurityDepositController {

    private final SecurityDepositService securityDepositService;

    @PostMapping
    public ResponseEntity<SecurityDepositResponse> holdDeposit(@Valid @RequestBody SecurityDepositRequest request) {
        return ResponseEntity.ok(securityDepositService.holdDeposit(request));
    }

    @PostMapping("/refund")
    public ResponseEntity<SecurityDepositResponse> refundDeposit(@Valid @RequestBody RefundDepositRequest request) {
        return ResponseEntity.ok(securityDepositService.refundDeposit(request));
    }

    @GetMapping("/lease/{leaseId}")
    public ResponseEntity<SecurityDepositResponse> getDepositByLease(@PathVariable String leaseId) {
        return ResponseEntity.ok(securityDepositService.getDepositByLease(leaseId));
    }
}