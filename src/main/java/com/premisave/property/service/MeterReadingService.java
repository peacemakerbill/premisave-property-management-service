package com.premisave.property.service;

import com.premisave.property.dto.request.MeterReadingRequest;
import com.premisave.property.dto.response.MeterReadingResponse;
import com.premisave.property.entity.MeterReading;
import com.premisave.property.exception.BadRequestException;
import com.premisave.property.exception.ResourceNotFoundException;
import com.premisave.property.repository.MeterReadingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MeterReadingService {

    private final MeterReadingRepository meterReadingRepository;

    @Transactional
    public MeterReadingResponse recordReading(MeterReadingRequest request, String tenantId) {
        if (request.getRentalUnitId() == null || request.getRentalUnitId().isBlank()) {
            throw new BadRequestException("rentalUnitId is required");
        }
        if (request.getCurrentReading() == null) {
            throw new BadRequestException("currentReading is required");
        }

        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);

        boolean alreadyRecordedToday = meterReadingRepository
                .existsByRentalUnitIdAndMeterTypeAndReadingDateBetween(
                        request.getRentalUnitId(), request.getMeterType(), startOfDay, endOfDay);

        if (alreadyRecordedToday) {
            throw new BadRequestException(
                    "A meter reading for this unit and meter type has already been recorded today");
        }

        BigDecimal previousReading = meterReadingRepository
                .findFirstByRentalUnitIdAndMeterTypeOrderByReadingDateDesc(
                        request.getRentalUnitId(), request.getMeterType())
                .map(MeterReading::getCurrentReading)
                .orElse(BigDecimal.ZERO);

        if (request.getCurrentReading().compareTo(previousReading) < 0) {
            throw new BadRequestException("currentReading cannot be less than the previous reading");
        }

        MeterReading reading = new MeterReading();
        reading.setRentalUnitId(request.getRentalUnitId());
        reading.setTenantId(tenantId);
        reading.setMeterType(request.getMeterType());
        reading.setPreviousReading(previousReading);
        reading.setCurrentReading(request.getCurrentReading());
        reading.setConsumption(request.getCurrentReading().subtract(previousReading));

        return toResponse(meterReadingRepository.save(reading));
    }

    public MeterReadingResponse getReading(String id) {
        return toResponse(findOrThrow(id));
    }

    public List<MeterReadingResponse> getReadingsByUnit(String rentalUnitId) {
        return meterReadingRepository.findByRentalUnitId(rentalUnitId).stream()
                .map(this::toResponse)
                .toList();
    }

    private MeterReading findOrThrow(String id) {
        return meterReadingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Meter reading not found"));
    }

    private MeterReadingResponse toResponse(MeterReading reading) {
        MeterReadingResponse response = new MeterReadingResponse();
        response.setId(reading.getId());
        response.setRentalUnitId(reading.getRentalUnitId());
        response.setMeterType(reading.getMeterType());
        response.setPreviousReading(reading.getPreviousReading());
        response.setCurrentReading(reading.getCurrentReading());
        response.setConsumption(reading.getConsumption());
        response.setReadingDate(reading.getReadingDate());
        return response;
    }
}