package com.premisave.property.controller;

import com.premisave.property.dto.request.MeterReadingRequest;
import com.premisave.property.dto.response.MeterReadingResponse;
import com.premisave.property.service.MeterReadingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/meter-readings")
@RequiredArgsConstructor
public class MeterReadingController {

    private final MeterReadingService meterReadingService;

    @PostMapping
    public ResponseEntity<MeterReadingResponse> recordReading(@Valid @RequestBody MeterReadingRequest request) {
        // tenantId from SecurityContext
        return ResponseEntity.ok(meterReadingService.recordReading(request, "tenant-id-from-jwt"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MeterReadingResponse> getReading(@PathVariable String id) {
        return ResponseEntity.ok(meterReadingService.getReading(id));
    }

    @GetMapping("/unit/{rentalUnitId}")
    public ResponseEntity<List<MeterReadingResponse>> getReadingsByUnit(@PathVariable String rentalUnitId) {
        return ResponseEntity.ok(meterReadingService.getReadingsByUnit(rentalUnitId));
    }

    @GetMapping("/unit/{rentalUnitId}/type/{meterType}")
    public ResponseEntity<List<MeterReadingResponse>> getReadingsByUnitAndType(
            @PathVariable String rentalUnitId, @PathVariable String meterType) {
        return ResponseEntity.ok(meterReadingService.getReadingsByUnitAndType(rentalUnitId, meterType));
    }
}