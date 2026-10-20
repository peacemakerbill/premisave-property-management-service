package com.premisave.property.controller;

import com.premisave.property.dto.request.MeterReadingRequest;
import com.premisave.property.dto.response.MeterReadingResponse;
import com.premisave.property.security.AccessGuard;
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
    private final AccessGuard guard;

    // The tenant living in the unit, or its manager, can record a reading.
    // tenantId is the recorder's tenant profile id when they have one, otherwise null.
    @PostMapping
    public ResponseEntity<MeterReadingResponse> recordReading(@Valid @RequestBody MeterReadingRequest request) {
        guard.requireUnitParticipant(request.getRentalUnitId());
        String tenantId = guard.currentTenantId().orElse(null);
        return ResponseEntity.ok(meterReadingService.recordReading(request, tenantId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MeterReadingResponse> getReading(@PathVariable String id) {
        MeterReadingResponse reading = meterReadingService.getReading(id);
        guard.requireUnitParticipant(reading.getRentalUnitId());
        return ResponseEntity.ok(reading);
    }

    @GetMapping("/unit/{rentalUnitId}")
    public ResponseEntity<List<MeterReadingResponse>> getReadingsByUnit(@PathVariable String rentalUnitId) {
        guard.requireUnitParticipant(rentalUnitId);
        return ResponseEntity.ok(meterReadingService.getReadingsByUnit(rentalUnitId));
    }

}
