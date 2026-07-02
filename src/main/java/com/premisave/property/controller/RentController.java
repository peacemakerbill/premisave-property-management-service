package com.premisave.property.controller;

import com.premisave.property.dto.request.RentPaymentRequest;
import com.premisave.property.dto.response.PaymentDueResponse;
import com.premisave.property.dto.response.RentPaymentResponse;
import com.premisave.property.service.RentPaymentService;
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

    @GetMapping("/due/{leaseId}")
    public ResponseEntity<PaymentDueResponse> getPaymentDue(@PathVariable String leaseId) {
        return ResponseEntity.ok(rentPaymentService.getPaymentDue(leaseId));
    }

    // TODO(WALLET-INTEGRATION): this currently books a payment as already
    // collected. Once the wallet service is connected, this endpoint (or a
    // new /initiate endpoint) should trigger the wallet service instead,
    // which handles M-Pesa STK Push / Stripe / PayPal, and only call
    // recordPayment() after the wallet confirms success.
    @PostMapping("/pay")
    public ResponseEntity<RentPaymentResponse> payRent(@Valid @RequestBody RentPaymentRequest request) {
        // tenantId from SecurityContext
        RentPaymentResponse response = rentPaymentService.recordPayment(request, "tenant-id-from-jwt");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/history/{leaseId}")
    public ResponseEntity<List<RentPaymentResponse>> getPaymentHistory(@PathVariable String leaseId) {
        return ResponseEntity.ok(rentPaymentService.getPaymentHistory(leaseId));
    }
}