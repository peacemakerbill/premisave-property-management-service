package com.premisave.property.service;

import com.premisave.property.dto.request.MeterReadingRequest;
import com.premisave.property.entity.MeterReading;
import com.premisave.property.repository.MeterReadingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MeterReadingService {

    private final MeterReadingRepository meterReadingRepository;

    public void recordReading(MeterReadingRequest request) {
        MeterReading reading = new MeterReading();
        // map fields
        meterReadingRepository.save(reading);
    }
}