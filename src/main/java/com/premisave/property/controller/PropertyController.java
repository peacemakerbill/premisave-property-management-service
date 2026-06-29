package com.premisave.property.controller;

import com.premisave.property.dto.request.CreatePropertyRequest;
import com.premisave.property.dto.response.PropertyResponse;
import com.premisave.property.service.PropertyService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/properties")
@RequiredArgsConstructor
public class PropertyController {

    private final PropertyService propertyService;

    @PostMapping
    @PreAuthorize("hasRole('HOME_OWNER')")
    @Operation(summary = "Create new property")
    public ResponseEntity<PropertyResponse> createProperty(@Valid @RequestBody CreatePropertyRequest request) {
        // ownerId from SecurityContext
        PropertyResponse response = propertyService.createProperty(request, "current-owner-id");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('HOME_OWNER')")
    public ResponseEntity<List<PropertyResponse>> getMyProperties() {
        List<PropertyResponse> properties = propertyService.getOwnerProperties("current-owner-id");
        return ResponseEntity.ok(properties);
    }
}