package com.premisave.property.controller;

import com.premisave.property.dto.request.LeaseRentPaymentRequest;
import com.premisave.property.dto.response.LeaseRentPaymentResponse;
import com.premisave.property.dto.response.PaymentDueResponse;
import com.premisave.property.service.LeaseRentUnitPaymentService;
import com.premisave.property.service.TenantService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Handles rent payments for LEASE-BACKED tenancies.
// For directly-occupied units with no lease, see UnitRentController instead.
@RestController
@RequestMapping("/api/v1/rent")
@RequiredArgsConstructor
public class LeaseRentController {

    private final LeaseRentUnitPaymentService leaseRentUnitPaymentService;
    private final TenantService tenantService;

    @GetMapping("/due/{leaseId}")
    public ResponseEntity<PaymentDueResponse> getPaymentDue(@PathVariable String leaseId) {
        return ResponseEntity.ok(leaseRentUnitPaymentService.getPaymentDue(leaseId));
    }

    @PostMapping("/pay")
    public ResponseEntity<LeaseRentPaymentResponse> payRent(@Valid @RequestBody LeaseRentPaymentRequest request,
                                                              HttpServletRequest httpRequest) {
        String tenantId = resolveTenantId(httpRequest);
        LeaseRentPaymentResponse response = leaseRentUnitPaymentService.recordPayment(request, tenantId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/history/{leaseId}")
    public ResponseEntity<List<LeaseRentPaymentResponse>> getPaymentHistory(@PathVariable String leaseId) {
        return ResponseEntity.ok(leaseRentUnitPaymentService.getPaymentHistory(leaseId));
    }

    private String resolveTenantId(HttpServletRequest httpRequest) {
        String userId = (String) httpRequest.getAttribute("userId");
        return tenantService.getTenantByUserId(userId).getId();
    }
}