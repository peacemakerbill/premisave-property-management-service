package com.premisave.property.controller;

import com.premisave.property.dto.request.MeterReadingRequest;
import com.premisave.property.service.MeterReadingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/meter-readings")
@RequiredArgsConstructor
public class MeterReadingController {

    private final MeterReadingService meterReadingService;

    @PostMapping
    public ResponseEntity<String> recordReading(@RequestBody MeterReadingRequest request) {
        meterReadingService.recordReading(request);
        return ResponseEntity.ok("Meter reading recorded successfully");
    }
}