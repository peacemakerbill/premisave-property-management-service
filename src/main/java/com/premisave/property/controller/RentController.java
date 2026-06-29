package com.premisave.property.controller;

import com.premisave.property.dto.request.RentPaymentRequest;
import com.premisave.property.dto.response.RentPaymentResponse;
import com.premisave.property.service.RentPaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/rent")
@RequiredArgsConstructor
public class RentController {

    private final RentPaymentService rentPaymentService;

    @PostMapping("/pay")
    public ResponseEntity<RentPaymentResponse> payRent(@RequestBody RentPaymentRequest request) {
        RentPaymentResponse response = rentPaymentService.recordPayment(request, "tenant-id");
        return ResponseEntity.ok(response);
    }
}