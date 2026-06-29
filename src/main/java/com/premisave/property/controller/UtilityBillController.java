package com.premisave.property.controller;

import com.premisave.property.dto.request.UtilityBillRequest;
import com.premisave.property.service.UtilityBillingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/utility-bills")
@RequiredArgsConstructor
public class UtilityBillController {

    private final UtilityBillingService utilityBillingService;

    @PostMapping
    public ResponseEntity<String> generateBill(@RequestBody UtilityBillRequest request) {
        utilityBillingService.generateBill(request);
        return ResponseEntity.ok("Utility bill generated successfully");
    }
}