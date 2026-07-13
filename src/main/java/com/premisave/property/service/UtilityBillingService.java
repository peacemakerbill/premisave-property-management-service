package com.premisave.property.service;

import com.premisave.property.config.UtilityRatesProperties;
import com.premisave.property.dto.request.GenerateBillFromReadingRequest;
import com.premisave.property.dto.request.PayUtilityBillRequest;
import com.premisave.property.dto.request.UtilityBillRequest;
import com.premisave.property.dto.response.UtilityBillResponse;
import com.premisave.property.entity.MeterReading;
import com.premisave.property.entity.OccupancyHistory;
import com.premisave.property.entity.UtilityBill;
import com.premisave.property.enums.MeterType;
import com.premisave.property.enums.PaymentStatus;
import com.premisave.property.enums.UtilityType;
import com.premisave.property.exception.BadRequestException;
import com.premisave.property.exception.ResourceNotFoundException;
import com.premisave.property.repository.MeterReadingRepository;
import com.premisave.property.repository.OccupancyHistoryRepository;
import com.premisave.property.repository.UtilityBillRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UtilityBillingService {

    private final UtilityBillRepository utilityBillRepository;
    private final OccupancyHistoryRepository occupancyHistoryRepository;
    private final MeterReadingRepository meterReadingRepository;
    private final UtilityRatesProperties utilityRatesProperties;

    @Transactional
    public UtilityBillResponse generateBill(UtilityBillRequest request) {
        String tenantId = resolveCurrentTenant(request.getRentalUnitId());

        UtilityBill bill = new UtilityBill();
        bill.setTenantId(tenantId);
        bill.setRentalUnitId(request.getRentalUnitId());
        bill.setUtilityType(request.getUtilityType());
        bill.setAmount(request.getAmount());
        bill.setAmountPaid(BigDecimal.ZERO);
        bill.setStatus(PaymentStatus.PENDING);
        bill.setBillingPeriodStart(request.getBillingPeriodStart());
        bill.setBillingPeriodEnd(request.getBillingPeriodEnd());

        return toResponse(utilityBillRepository.save(bill));
    }

    @Transactional
    public UtilityBillResponse generateBillFromReading(GenerateBillFromReadingRequest request) {
        MeterReading reading = meterReadingRepository.findById(request.getMeterReadingId())
                .orElseThrow(() -> new ResourceNotFoundException("Meter reading not found"));

        if (reading.getConsumption() == null) {
            throw new BadRequestException("Meter reading has no recorded consumption");
        }

        String tenantId = resolveCurrentTenant(reading.getRentalUnitId());
        UtilityType utilityType = resolveUtilityType(reading.getMeterType());
        BigDecimal ratePerUnit = resolveRatePerUnit(utilityType, request.getRatePerUnit());
        BigDecimal amount = reading.getConsumption().multiply(ratePerUnit);

        UtilityBill bill = new UtilityBill();
        bill.setTenantId(tenantId);
        bill.setRentalUnitId(reading.getRentalUnitId());
        bill.setUtilityType(utilityType);
        bill.setAmount(amount);
        bill.setAmountPaid(BigDecimal.ZERO);
        bill.setStatus(PaymentStatus.PENDING);
        bill.setBillingPeriodEnd(reading.getReadingDate());

        return toResponse(utilityBillRepository.save(bill));
    }

    @Transactional
    public UtilityBillResponse payBill(PayUtilityBillRequest request) {
        UtilityBill bill = findOrThrow(request.getBillId());

        BigDecimal newAmountPaid = bill.getAmountPaid().add(request.getAmount());
        bill.setAmountPaid(newAmountPaid);
        bill.setStatus(newAmountPaid.compareTo(bill.getAmount()) >= 0
                ? PaymentStatus.PAID
                : PaymentStatus.PARTIALLY_PAID);

        return toResponse(utilityBillRepository.save(bill));
    }

    public UtilityBillResponse getBill(String id) {
        return toResponse(findOrThrow(id));
    }

    public List<UtilityBillResponse> getBillsByUnit(String rentalUnitId) {
        return utilityBillRepository.findByRentalUnitId(rentalUnitId).stream()
                .map(this::toResponse)
                .toList();
    }

    public List<UtilityBillResponse> getBillsByTenant(String tenantId) {
        return utilityBillRepository.findByTenantId(tenantId).stream()
                .map(this::toResponse)
                .toList();
    }

    public List<UtilityBillResponse> getOutstandingBillsByTenant(String tenantId) {
        return utilityBillRepository.findByTenantIdAndStatusIn(tenantId,
                        List.of(PaymentStatus.PENDING, PaymentStatus.PARTIALLY_PAID, PaymentStatus.OVERDUE)).stream()
                .map(this::toResponse)
                .toList();
    }

    private String resolveCurrentTenant(String rentalUnitId) {
        return occupancyHistoryRepository.findByRentalUnitIdAndMoveOutDateIsNull(rentalUnitId)
                .map(OccupancyHistory::getTenantId)
                .orElseThrow(() -> new BadRequestException("This unit currently has no active tenant"));
    }

    private UtilityType resolveUtilityType(MeterType meterType) {
        try {
            return UtilityType.valueOf(meterType.name());
        } catch (IllegalArgumentException ex) {
            return UtilityType.OTHER;
        }
    }

    private BigDecimal resolveRatePerUnit(UtilityType utilityType, BigDecimal requestedRate) {
        if (requestedRate != null) {
            return requestedRate;
        }

        BigDecimal configuredRate = utilityRatesProperties.getRateFor(utilityType);
        if (configuredRate == null) {
            throw new BadRequestException(
                    "No configured rate for utility type " + utilityType
                            + "; please provide a ratePerUnit override");
        }

        return configuredRate;
    }

    private UtilityBill findOrThrow(String id) {
        return utilityBillRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utility bill not found"));
    }

    private UtilityBillResponse toResponse(UtilityBill bill) {
        UtilityBillResponse response = new UtilityBillResponse();
        response.setId(bill.getId());
        response.setTenantId(bill.getTenantId());
        response.setRentalUnitId(bill.getRentalUnitId());
        response.setUtilityType(bill.getUtilityType());
        response.setAmount(bill.getAmount());
        response.setAmountPaid(bill.getAmountPaid());
        response.setStatus(bill.getStatus());
        response.setBillingPeriodStart(bill.getBillingPeriodStart());
        response.setBillingPeriodEnd(bill.getBillingPeriodEnd());
        return response;
    }
}