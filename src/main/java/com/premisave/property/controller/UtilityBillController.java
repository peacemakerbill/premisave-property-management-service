package com.premisave.property.controller;

import com.premisave.property.dto.request.GenerateBillFromReadingRequest;
import com.premisave.property.dto.request.PayUtilityBillRequest;
import com.premisave.property.dto.request.UtilityBillRequest;
import com.premisave.property.dto.response.UtilityBillResponse;
import com.premisave.property.service.UtilityBillingService;
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

    @PostMapping
    public ResponseEntity<UtilityBillResponse> generateBill(@Valid @RequestBody UtilityBillRequest request) {
        return ResponseEntity.ok(utilityBillingService.generateBill(request));
    }

    @PostMapping("/from-reading")
    public ResponseEntity<UtilityBillResponse> generateBillFromReading(
            @Valid @RequestBody GenerateBillFromReadingRequest request) {
        return ResponseEntity.ok(utilityBillingService.generateBillFromReading(request));
    }

    // TODO(WALLET-INTEGRATION): same as rent payment — this books a payment as
    // already collected. Should route through the wallet service once connected.
    @PostMapping("/pay")
    public ResponseEntity<UtilityBillResponse> payBill(@Valid @RequestBody PayUtilityBillRequest request) {
        return ResponseEntity.ok(utilityBillingService.payBill(request));
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
}