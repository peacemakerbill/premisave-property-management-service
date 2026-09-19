package com.premisave.property.controller;

import com.premisave.property.dto.request.GenerateBillFromReadingRequest;
import com.premisave.property.dto.request.PayUtilityBillRequest;
import com.premisave.property.dto.request.UtilityBillRequest;
import com.premisave.property.dto.response.UtilityBillResponse;
import com.premisave.property.service.TenantService;
import com.premisave.property.service.UtilityBillingService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/utility-bills")
@RequiredArgsConstructor
public class UtilityBillController {

    private final UtilityBillingService utilityBillingService;
    private final TenantService tenantService;

    @PostMapping
    public ResponseEntity<UtilityBillResponse> generateBill(@Valid @RequestBody UtilityBillRequest request) {
        return ResponseEntity.ok(utilityBillingService.generateBill(request));
    }

    @PostMapping("/from-reading")
    public ResponseEntity<UtilityBillResponse> generateBillFromReading(
            @Valid @RequestBody GenerateBillFromReadingRequest request) {
        return ResponseEntity.ok(utilityBillingService.generateBillFromReading(request));
    }

    // Paid from the calling tenant's wallet — the tenant comes from the JWT,
    // never from the request body.
    @PostMapping("/pay")
    public ResponseEntity<UtilityBillResponse> payBill(@Valid @RequestBody PayUtilityBillRequest request,
                                                         HttpServletRequest httpRequest) {
        String tenantId = resolveTenantId(httpRequest);
        return ResponseEntity.ok(utilityBillingService.payBill(request, tenantId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UtilityBillResponse> getBill(@PathVariable String id) {
        return ResponseEntity.ok(utilityBillingService.getBill(id));
    }

    @GetMapping("/unit/{rentalUnitId}")
    public ResponseEntity<List<UtilityBillResponse>> getBillsByUnit(@PathVariable String rentalUnitId) {
        return ResponseEntity.ok(utilityBillingService.getBillsByUnit(rentalUnitId));
    }

    @GetMapping("/tenant/{tenantId}")
    public ResponseEntity<List<UtilityBillResponse>> getBillsByTenant(@PathVariable String tenantId) {
        return ResponseEntity.ok(utilityBillingService.getBillsByTenant(tenantId));
    }

    @GetMapping("/tenant/{tenantId}/outstanding")
    public ResponseEntity<List<UtilityBillResponse>> getOutstandingBillsByTenant(@PathVariable String tenantId) {
        return ResponseEntity.ok(utilityBillingService.getOutstandingBillsByTenant(tenantId));
    }

    private String resolveTenantId(HttpServletRequest httpRequest) {
        String userId = (String) httpRequest.getAttribute("userId");
        return tenantService.getTenantByUserId(userId).getId();
    }
}