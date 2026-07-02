package com.premisave.property.controller;

import com.premisave.property.dto.request.RentalUnitRequest;
import com.premisave.property.dto.response.RentalUnitResponse;
import com.premisave.property.service.RentalUnitService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/units")
@RequiredArgsConstructor
public class RentalUnitController {

    private final RentalUnitService rentalUnitService;

    @PostMapping("/property/{propertyId}")
    public ResponseEntity<RentalUnitResponse> createUnit(@PathVariable String propertyId,
                                                           @Valid @RequestBody RentalUnitRequest request) {
        return ResponseEntity.ok(rentalUnitService.createUnit(propertyId, request));
    }

    @GetMapping("/property/{propertyId}")
    public ResponseEntity<List<RentalUnitResponse>> getUnitsByProperty(@PathVariable String propertyId) {
        return ResponseEntity.ok(rentalUnitService.getUnitsByProperty(propertyId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RentalUnitResponse> getUnit(@PathVariable String id) {
        return ResponseEntity.ok(rentalUnitService.getUnitById(id));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<RentalUnitResponse> updateUnit(@PathVariable String id,
                                                           @RequestBody RentalUnitRequest request) {
        return ResponseEntity.ok(rentalUnitService.updateUnit(id, request));
    }
}