package com.premisave.property.controller;

import com.premisave.property.dto.request.CreateOwnerRequest;
import com.premisave.property.dto.response.OwnerResponse;
import com.premisave.property.service.OwnerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/owners")
@RequiredArgsConstructor
public class OwnerController {

    private final OwnerService ownerService;

    @PostMapping
    public ResponseEntity<OwnerResponse> createOwner(@RequestBody CreateOwnerRequest request) {
        OwnerResponse owner = ownerService.createOwner(request, "user-id-from-jwt");
        return ResponseEntity.ok(owner);
    }
}