package com.premisave.property.controller;

import com.premisave.property.dto.request.RefundDepositRequest;
import com.premisave.property.dto.request.SecurityDepositRequest;
import com.premisave.property.dto.response.RefundCheckResponse;
import com.premisave.property.dto.response.SecurityDepositResponse;
import com.premisave.property.security.AccessGuard;
import com.premisave.property.service.SecurityDepositService;
import com.premisave.property.service.TenantService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/security-deposits")
@RequiredArgsConstructor
public class SecurityDepositController {

    private final SecurityDepositService securityDepositService;
    private final TenantService tenantService;
    private final AccessGuard guard;

    @PostMapping
    public ResponseEntity<SecurityDepositResponse> holdDeposit(@Valid @RequestBody SecurityDepositRequest request) {
        requireManagerOfDeposit(request.getLeaseId(), request.getRentalUnitId());
        return ResponseEntity.ok(securityDepositService.holdDeposit(request));
    }

    @PostMapping("/refund")
    public ResponseEntity<SecurityDepositResponse> refundDeposit(@Valid @RequestBody RefundDepositRequest request) {
        requireManagerOfDeposit(request.getLeaseId(), request.getRentalUnitId());
        return ResponseEntity.ok(securityDepositService.refundDeposit(request));
    }

    @GetMapping("/lease/{leaseId}/refund-check")
    public ResponseEntity<RefundCheckResponse> checkLeaseRefund(@PathVariable String leaseId,
                                                                  @RequestParam BigDecimal amount) {
        guard.requireLeaseManager(leaseId);
        return ResponseEntity.ok(securityDepositService.checkRefund(leaseId, null, null, amount));
    }

    @GetMapping("/unit/{rentalUnitId}/tenant/{tenantId}/refund-check")
    public ResponseEntity<RefundCheckResponse> checkUnitRefund(@PathVariable String rentalUnitId,
                                                                 @PathVariable String tenantId,
                                                                 @RequestParam BigDecimal amount) {
        guard.requireUnitManager(rentalUnitId);
        return ResponseEntity.ok(securityDepositService.checkRefund(null, rentalUnitId, tenantId, amount));
    }

    @GetMapping("/lease/{leaseId}")
    public ResponseEntity<SecurityDepositResponse> getDepositByLease(@PathVariable String leaseId) {
        guard.requireLeaseParticipant(leaseId);
        return ResponseEntity.ok(securityDepositService.getDepositByLease(leaseId));
    }

    // The tenant themselves, or whoever manages the unit.
    @GetMapping("/unit/{rentalUnitId}/tenant/{tenantId}")
    public ResponseEntity<SecurityDepositResponse> getDepositByUnitForTenant(@PathVariable String rentalUnitId,
                                                                              @PathVariable String tenantId) {
        if (!guard.isSelf(tenantId)) {
            guard.requireUnitManager(rentalUnitId);
        }
        return ResponseEntity.ok(securityDepositService.getDepositByUnit(rentalUnitId, tenantId));
    }

    @GetMapping("/unit/{rentalUnitId}/me")
    public ResponseEntity<SecurityDepositResponse> getMyDepositByUnit(@PathVariable String rentalUnitId,
                                                                       HttpServletRequest httpRequest) {
        String tenantId = resolveTenantId(httpRequest);
        return ResponseEntity.ok(securityDepositService.getDepositByUnit(rentalUnitId, tenantId));
    }

    // Holding or refunding a deposit is the landlord's side of the tenancy.
    private void requireManagerOfDeposit(String leaseId, String rentalUnitId) {
        if (leaseId != null && !leaseId.isBlank()) {
            guard.requireLeaseManager(leaseId);
        } else if (rentalUnitId != null && !rentalUnitId.isBlank()) {
            guard.requireUnitManager(rentalUnitId);
        }
        // neither given: the service rejects the request as a 400
    }

    private String resolveTenantId(HttpServletRequest httpRequest) {
        String userId = (String) httpRequest.getAttribute("userId");
        return tenantService.getTenantByUserId(userId).getId();
    }
}
