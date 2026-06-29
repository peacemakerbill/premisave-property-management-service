package com.premisave.property.controller;

import com.premisave.property.dto.request.SecurityDepositRequest;
import com.premisave.property.service.SecurityDepositService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/security-deposits")
@RequiredArgsConstructor
public class SecurityDepositController {

    private final SecurityDepositService securityDepositService;

    @PostMapping
    public ResponseEntity<String> holdDeposit(@RequestBody SecurityDepositRequest request) {
        securityDepositService.holdDeposit(request);
        return ResponseEntity.ok("Security deposit held successfully");
    }
}