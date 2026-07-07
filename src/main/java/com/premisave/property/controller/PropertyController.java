package com.premisave.property.controller;

import com.premisave.property.dto.request.CreatePropertyRequest;
import com.premisave.property.dto.request.UpdatePropertyRequest;
import com.premisave.property.dto.response.PropertyResponse;
import com.premisave.property.service.OwnerService;
import com.premisave.property.service.PropertyService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
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
    private final OwnerService ownerService;

    @PostMapping
    @PreAuthorize("hasRole('HOME_OWNER')")
    @Operation(summary = "Create new property")
    public ResponseEntity<PropertyResponse> createProperty(@Valid @RequestBody CreatePropertyRequest request,
                                                             HttpServletRequest httpRequest) {
        String ownerId = resolveOwnerId(httpRequest);
        PropertyResponse response = propertyService.createProperty(request, ownerId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('HOME_OWNER')")
    public ResponseEntity<List<PropertyResponse>> getMyProperties(HttpServletRequest httpRequest) {
        String ownerId = resolveOwnerId(httpRequest);
        List<PropertyResponse> properties = propertyService.getOwnerProperties(ownerId);
        return ResponseEntity.ok(properties);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PropertyResponse> getProperty(@PathVariable String id) {
        return ResponseEntity.ok(propertyService.getPropertyById(id));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('HOME_OWNER')")
    public ResponseEntity<PropertyResponse> updateProperty(@PathVariable String id,
                                                             @RequestBody UpdatePropertyRequest request,
                                                             HttpServletRequest httpRequest) {
        String ownerId = resolveOwnerId(httpRequest);
        return ResponseEntity.ok(propertyService.updateProperty(id, request, ownerId));
    }

    private String resolveOwnerId(HttpServletRequest httpRequest) {
        String userId = (String) httpRequest.getAttribute("userId");
        return ownerService.getOwnerByUserId(userId).getId();
    }
}