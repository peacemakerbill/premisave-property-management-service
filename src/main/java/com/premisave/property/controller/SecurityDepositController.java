package com.premisave.property.controller;

import com.premisave.property.dto.request.RefundDepositRequest;
import com.premisave.property.dto.request.SecurityDepositRequest;
import com.premisave.property.dto.response.SecurityDepositResponse;
import com.premisave.property.service.SecurityDepositService;
import com.premisave.property.service.TenantService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/security-deposits")
@RequiredArgsConstructor
public class SecurityDepositController {

    private final SecurityDepositService securityDepositService;
    private final TenantService tenantService;

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

    @GetMapping("/unit/{rentalUnitId}/tenant/{tenantId}")
    public ResponseEntity<SecurityDepositResponse> getDepositByUnitForTenant(@PathVariable String rentalUnitId,
                                                                              @PathVariable String tenantId) {
        return ResponseEntity.ok(securityDepositService.getDepositByUnit(rentalUnitId, tenantId));
    }

    @GetMapping("/unit/{rentalUnitId}/me")
    public ResponseEntity<SecurityDepositResponse> getMyDepositByUnit(@PathVariable String rentalUnitId,
                                                                       HttpServletRequest httpRequest) {
        String tenantId = resolveTenantId(httpRequest);
        return ResponseEntity.ok(securityDepositService.getDepositByUnit(rentalUnitId, tenantId));
    }

    private String resolveTenantId(HttpServletRequest httpRequest) {
        String userId = (String) httpRequest.getAttribute("userId");
        return tenantService.getTenantByUserId(userId).getId();
    }
}