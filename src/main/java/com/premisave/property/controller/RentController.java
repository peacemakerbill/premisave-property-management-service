package com.premisave.property.controller;

import com.premisave.property.dto.request.RentPaymentRequest;
import com.premisave.property.dto.response.PaymentDueResponse;
import com.premisave.property.dto.response.RentPaymentResponse;
import com.premisave.property.service.RentPaymentService;
import com.premisave.property.service.TenantService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/rent")
@RequiredArgsConstructor
public class RentController {

    private final RentPaymentService rentPaymentService;
    private final TenantService tenantService;

    @GetMapping("/due/{leaseId}")
    public ResponseEntity<PaymentDueResponse> getPaymentDue(@PathVariable String leaseId) {
        return ResponseEntity.ok(rentPaymentService.getPaymentDue(leaseId));
    }

    @PostMapping("/pay")
    public ResponseEntity<RentPaymentResponse> payRent(@Valid @RequestBody RentPaymentRequest request,
                                                         HttpServletRequest httpRequest) {
        String tenantId = resolveTenantId(httpRequest);
        RentPaymentResponse response = rentPaymentService.recordPayment(request, tenantId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/history/{leaseId}")
    public ResponseEntity<List<RentPaymentResponse>> getPaymentHistory(@PathVariable String leaseId) {
        return ResponseEntity.ok(rentPaymentService.getPaymentHistory(leaseId));
    }

    private String resolveTenantId(HttpServletRequest httpRequest) {
        String userId = (String) httpRequest.getAttribute("userId");
        return tenantService.getTenantByUserId(userId).getId();
    }
}