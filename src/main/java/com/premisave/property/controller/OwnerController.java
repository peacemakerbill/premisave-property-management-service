package com.premisave.property.controller;

import com.premisave.property.dto.request.CreateOwnerRequest;
import com.premisave.property.dto.request.UpdateOwnerRequest;
import com.premisave.property.dto.response.OwnerResponse;
import com.premisave.property.service.OwnerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/owners")
@RequiredArgsConstructor
public class OwnerController {

    private final OwnerService ownerService;

    @PostMapping
    public ResponseEntity<OwnerResponse> createOwner(@Valid @RequestBody CreateOwnerRequest request) {
        // userId from SecurityContext
        OwnerResponse owner = ownerService.createOwner(request, "user-id-from-jwt");
        return ResponseEntity.ok(owner);
    }

    @GetMapping("/me")
    public ResponseEntity<OwnerResponse> getMyOwnerProfile() {
        // userId from SecurityContext
        return ResponseEntity.ok(ownerService.getOwnerByUserId("user-id-from-jwt"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OwnerResponse> getOwner(@PathVariable String id) {
        return ResponseEntity.ok(ownerService.getOwnerById(id));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<OwnerResponse> updateOwner(@PathVariable String id,
                                                       @RequestBody UpdateOwnerRequest request) {
        // userId from SecurityContext
        return ResponseEntity.ok(ownerService.updateOwner(id, request, "user-id-from-jwt"));
    }
}