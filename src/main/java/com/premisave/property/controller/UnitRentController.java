package com.premisave.property.controller;

import com.premisave.property.dto.request.UnitRentPaymentRequest;
import com.premisave.property.dto.response.PaymentDueResponse;
import com.premisave.property.dto.response.UnitRentPaymentResponse;
import com.premisave.property.service.TenantService;
import com.premisave.property.service.UnitRentPaymentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Handles rent payments for DIRECTLY OCCUPIED rental units (no lease).
// For lease-backed tenancies, use LeaseRentController's /api/v1/rent/pay instead.
@RestController
@RequestMapping("/api/v1/rent/units")
@RequiredArgsConstructor
public class UnitRentController {

    private final UnitRentPaymentService unitRentPaymentService;
    private final TenantService tenantService;

    @GetMapping("/due/{rentalUnitId}")
    public ResponseEntity<PaymentDueResponse> getMyPaymentDue(@PathVariable String rentalUnitId,
                                                               HttpServletRequest httpRequest) {
        String tenantId = resolveTenantId(httpRequest);
        return ResponseEntity.ok(unitRentPaymentService.getPaymentDue(rentalUnitId, tenantId));
    }

    @GetMapping("/due/{rentalUnitId}/tenant/{tenantId}")
    public ResponseEntity<PaymentDueResponse> getPaymentDueForTenant(@PathVariable String rentalUnitId,
                                                                      @PathVariable String tenantId) {
        return ResponseEntity.ok(unitRentPaymentService.getPaymentDue(rentalUnitId, tenantId));
    }

    @PostMapping("/pay")
    public ResponseEntity<UnitRentPaymentResponse> payUnitRent(@Valid @RequestBody UnitRentPaymentRequest request,
                                                                 HttpServletRequest httpRequest) {
        String tenantId = resolveTenantId(httpRequest);
        return ResponseEntity.ok(unitRentPaymentService.recordPayment(request, tenantId));
    }

    @GetMapping("/history/{rentalUnitId}")
    public ResponseEntity<List<UnitRentPaymentResponse>> getPaymentHistory(@PathVariable String rentalUnitId) {
        return ResponseEntity.ok(unitRentPaymentService.getPaymentHistory(rentalUnitId));
    }

    private String resolveTenantId(HttpServletRequest httpRequest) {
        String userId = (String) httpRequest.getAttribute("userId");
        return tenantService.getTenantByUserId(userId).getId();
    }
}