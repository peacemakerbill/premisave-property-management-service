package com.premisave.property.controller;

import com.premisave.property.dto.response.RentBalanceResponse;
import com.premisave.property.dto.response.TenantRentBalanceSummaryResponse;
import com.premisave.property.service.RentBalanceService;
import com.premisave.property.service.TenantService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// Arrears / credit reporting — the "fee balance statement" view, covering
// both lease-based and direct-unit tenancies.
@RestController
@RequestMapping("/api/v1/rent/balance")
@RequiredArgsConstructor
public class RentBalanceController {

    private final RentBalanceService rentBalanceService;
    private final TenantService tenantService;

    @GetMapping("/lease/{leaseId}")
    public ResponseEntity<RentBalanceResponse> getLeaseBalance(@PathVariable String leaseId) {
        return ResponseEntity.ok(rentBalanceService.getLeaseBalance(leaseId));
    }

    @GetMapping("/unit/{rentalUnitId}/me")
    public ResponseEntity<RentBalanceResponse> getMyUnitBalance(@PathVariable String rentalUnitId,
                                                                  HttpServletRequest httpRequest) {
        String tenantId = resolveTenantId(httpRequest);
        return ResponseEntity.ok(rentBalanceService.getUnitBalance(rentalUnitId, tenantId));
    }

    @GetMapping("/unit/{rentalUnitId}/tenant/{tenantId}")
    public ResponseEntity<RentBalanceResponse> getUnitBalanceForTenant(@PathVariable String rentalUnitId,
                                                                         @PathVariable String tenantId) {
        return ResponseEntity.ok(rentBalanceService.getUnitBalance(rentalUnitId, tenantId));
    }

    @GetMapping("/tenant/{tenantId}")
    public ResponseEntity<TenantRentBalanceSummaryResponse> getTenantSummary(@PathVariable String tenantId) {
        return ResponseEntity.ok(rentBalanceService.getTenantSummary(tenantId));
    }

    @GetMapping("/me")
    public ResponseEntity<TenantRentBalanceSummaryResponse> getMySummary(HttpServletRequest httpRequest) {
        String tenantId = resolveTenantId(httpRequest);
        return ResponseEntity.ok(rentBalanceService.getTenantSummary(tenantId));
    }

    private String resolveTenantId(HttpServletRequest httpRequest) {
        String userId = (String) httpRequest.getAttribute("userId");
        return tenantService.getTenantByUserId(userId).getId();
    }
}