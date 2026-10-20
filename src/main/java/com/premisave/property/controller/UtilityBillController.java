package com.premisave.property.controller;

import com.premisave.property.dto.request.GenerateBillFromReadingRequest;
import com.premisave.property.dto.request.PayUtilityBillRequest;
import com.premisave.property.dto.request.UtilityBillRequest;
import com.premisave.property.dto.response.UtilityBillResponse;
import com.premisave.property.security.AccessGuard;
import com.premisave.property.service.TenantService;
import com.premisave.property.service.UtilityBillingService;
import com.premisave.property.health.ExternalService;
import com.premisave.property.health.RequiresServices;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/utility-bills")
@RequiredArgsConstructor
public class UtilityBillController {

    private final UtilityBillingService utilityBillingService;
    private final TenantService tenantService;
    private final AccessGuard guard;

    @PostMapping
    public ResponseEntity<UtilityBillResponse> generateBill(@Valid @RequestBody UtilityBillRequest request) {
        guard.requireUnitManager(request.getRentalUnitId());
        return ResponseEntity.ok(utilityBillingService.generateBill(request));
    }

    @PostMapping("/from-reading")
    public ResponseEntity<UtilityBillResponse> generateBillFromReading(
            @Valid @RequestBody GenerateBillFromReadingRequest request) {
        guard.requireMeterReadingManager(request.getMeterReadingId());
        return ResponseEntity.ok(utilityBillingService.generateBillFromReading(request));
    }

    // Paid from the calling tenant's wallet — the tenant comes from the JWT,
    // never from the request body.
    @RequiresServices(value = {ExternalService.WALLET, ExternalService.AUTH},
            action = "process payments", reassurance = "Nothing has been charged.")
    @PostMapping("/pay")
    public ResponseEntity<UtilityBillResponse> payBill(@Valid @RequestBody PayUtilityBillRequest request,
                                                         HttpServletRequest httpRequest) {
        String tenantId = resolveTenantId(httpRequest);
        return ResponseEntity.ok(utilityBillingService.payBill(request, tenantId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UtilityBillResponse> getBill(@PathVariable String id) {
        UtilityBillResponse bill = utilityBillingService.getBill(id);
        if (!guard.isSelf(bill.getTenantId())) {
            guard.requireUnitManager(bill.getRentalUnitId());
        }
        return ResponseEntity.ok(bill);
    }

    @GetMapping("/unit/{rentalUnitId}")
    public ResponseEntity<List<UtilityBillResponse>> getBillsByUnit(@PathVariable String rentalUnitId) {
        guard.requireUnitManager(rentalUnitId);
        return ResponseEntity.ok(utilityBillingService.getBillsByUnit(rentalUnitId));
    }

    @GetMapping("/tenant/{tenantId}")
    public ResponseEntity<List<UtilityBillResponse>> getBillsByTenant(@PathVariable String tenantId) {
        guard.requireTenantSelfOrLandlord(tenantId);
        return ResponseEntity.ok(visibleTo(tenantId, utilityBillingService.getBillsByTenant(tenantId)));
    }

    @GetMapping("/tenant/{tenantId}/outstanding")
    public ResponseEntity<List<UtilityBillResponse>> getOutstandingBillsByTenant(@PathVariable String tenantId) {
        guard.requireTenantSelfOrLandlord(tenantId);
        return ResponseEntity.ok(visibleTo(tenantId, utilityBillingService.getOutstandingBillsByTenant(tenantId)));
    }

    // The tenant sees all of their bills. A landlord sees only the bills for their own units.
    private List<UtilityBillResponse> visibleTo(String tenantId, List<UtilityBillResponse> bills) {
        if (guard.seesAllOf(tenantId)) {
            return bills;
        }
        Set<String> unitIds = guard.ownedUnitIds();
        return bills.stream().filter(b -> unitIds.contains(b.getRentalUnitId())).toList();
    }

    private String resolveTenantId(HttpServletRequest httpRequest) {
        String userId = (String) httpRequest.getAttribute("userId");
        return tenantService.getTenantByUserId(userId).getId();
    }
}
