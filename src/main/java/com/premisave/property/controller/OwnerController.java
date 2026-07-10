package com.premisave.property.controller;

import com.premisave.property.dto.request.CreateOwnerRequest;
import com.premisave.property.dto.request.UpdateOwnerRequest;
import com.premisave.property.dto.response.OwnerResponse;
import com.premisave.property.service.OwnerService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/owners")
@RequiredArgsConstructor
public class OwnerController {

    private final OwnerService ownerService;

    @PostMapping
    public ResponseEntity<OwnerResponse> createOwner(@Valid @RequestBody CreateOwnerRequest request,
                                                       HttpServletRequest httpRequest) {
        String userId = resolveUserId(httpRequest);
        OwnerResponse owner = ownerService.createOwner(request, userId);
        return ResponseEntity.ok(owner);
    }

    @GetMapping("/me")
    public ResponseEntity<OwnerResponse> getMyOwnerProfile(HttpServletRequest httpRequest) {
        String userId = resolveUserId(httpRequest);
        return ResponseEntity.ok(ownerService.getOwnerByUserId(userId));
    }

    @GetMapping("/me/exists")
    public ResponseEntity<Map<String, Boolean>> checkMyOwnerProfileExists(HttpServletRequest httpRequest) {
        String userId = resolveUserId(httpRequest);
        boolean exists = ownerService.existsByUserId(userId);
        return ResponseEntity.ok(Map.of("exists", exists));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OwnerResponse> getOwner(@PathVariable String id) {
        return ResponseEntity.ok(ownerService.getOwnerById(id));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<OwnerResponse> updateOwner(@PathVariable String id,
                                                       @RequestBody UpdateOwnerRequest request,
                                                       HttpServletRequest httpRequest) {
        String userId = resolveUserId(httpRequest);
        return ResponseEntity.ok(ownerService.updateOwner(id, request, userId));
    }

    private String resolveUserId(HttpServletRequest httpRequest) {
        return (String) httpRequest.getAttribute("userId");
    }
}