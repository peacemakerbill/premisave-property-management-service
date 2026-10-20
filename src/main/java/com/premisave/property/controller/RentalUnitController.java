package com.premisave.property.controller;

import com.premisave.property.dto.request.RentalUnitRequest;
import com.premisave.property.dto.response.RentalUnitResponse;
import com.premisave.property.security.AccessGuard;
import com.premisave.property.service.RentalUnitService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/units")
@RequiredArgsConstructor
public class RentalUnitController {

    private final RentalUnitService rentalUnitService;
    private final AccessGuard guard;

    @PostMapping("/property/{propertyId}")
    public ResponseEntity<RentalUnitResponse> createUnit(@PathVariable String propertyId,
                                                           @Valid @RequestBody RentalUnitRequest request) {
        guard.requirePropertyManager(propertyId);
        return ResponseEntity.ok(rentalUnitService.createUnit(propertyId, request));
    }

    // The property's manager, or a tenant who rents part of it.
    @GetMapping("/property/{propertyId}")
    public ResponseEntity<List<RentalUnitResponse>> getUnitsByProperty(@PathVariable String propertyId) {
        guard.requirePropertyParticipant(propertyId);
        return ResponseEntity.ok(rentalUnitService.getUnitsByProperty(propertyId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RentalUnitResponse> getUnit(@PathVariable String id) {
        guard.requireUnitParticipant(id);
        return ResponseEntity.ok(rentalUnitService.getUnitById(id));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<RentalUnitResponse> updateUnit(@PathVariable String id,
                                                           @RequestBody RentalUnitRequest request) {
        guard.requireUnitManager(id);
        return ResponseEntity.ok(rentalUnitService.updateUnit(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteUnit(@PathVariable String id) {
        guard.requireUnitManager(id);
        rentalUnitService.deleteUnit(id);
        return ResponseEntity.ok(Map.of("message", "Rental unit has been deleted successfully"));
    }
}
