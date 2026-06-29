package com.premisave.property.controller;

import com.premisave.property.dto.response.RentalUnitResponse;
import com.premisave.property.service.RentalUnitService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/units")
@RequiredArgsConstructor
public class RentalUnitController {

    private final RentalUnitService rentalUnitService;

    @GetMapping("/property/{propertyId}")
    public ResponseEntity<List<RentalUnitResponse>> getUnitsByProperty(@PathVariable String propertyId) {
        List<RentalUnitResponse> units = rentalUnitService.getUnitsByProperty(propertyId);
        return ResponseEntity.ok(units);
    }
}