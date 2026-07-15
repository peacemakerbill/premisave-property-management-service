package com.premisave.property.controller;

import com.premisave.property.dto.request.RentalUnitRentPaymentRequest;
import com.premisave.property.dto.response.UnitRentPaymentResponse;
import com.premisave.property.service.RentalUnitRentPaymentService;
import com.premisave.property.service.TenantService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Handles rent payments for DIRECTLY OCCUPIED rental units (no lease).
// For lease-backed tenancies, use RentController's /api/v1/rent/pay instead.
@RestController
@RequestMapping("/api/v1/rent/units")
@RequiredArgsConstructor
public class RentalUnitRentController {

    private final RentalUnitRentPaymentService rentalUnitRentPaymentService;
    private final TenantService tenantService;

    @PostMapping("/pay")
    public ResponseEntity<UnitRentPaymentResponse> payUnitRent(@Valid @RequestBody RentalUnitRentPaymentRequest request,
                                                                 HttpServletRequest httpRequest) {
        String tenantId = resolveTenantId(httpRequest);
        return ResponseEntity.ok(rentalUnitRentPaymentService.recordPayment(request, tenantId));
    }

    @GetMapping("/history/{rentalUnitId}")
    public ResponseEntity<List<UnitRentPaymentResponse>> getPaymentHistory(@PathVariable String rentalUnitId) {
        return ResponseEntity.ok(rentalUnitRentPaymentService.getPaymentHistory(rentalUnitId));
    }

    private String resolveTenantId(HttpServletRequest httpRequest) {
        String userId = (String) httpRequest.getAttribute("userId");
        return tenantService.getTenantByUserId(userId).getId();
    }
}