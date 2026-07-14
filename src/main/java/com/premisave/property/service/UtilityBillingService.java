package com.premisave.property.service;

import com.premisave.property.config.UtilityRatesProperties;
import com.premisave.property.dto.request.GenerateBillFromReadingRequest;
import com.premisave.property.dto.request.PayUtilityBillRequest;
import com.premisave.property.dto.request.UtilityBillRequest;
import com.premisave.property.dto.response.PropertySummaryResponse;
import com.premisave.property.dto.response.RentalUnitSummaryResponse;
import com.premisave.property.dto.response.TenantSummaryResponse;
import com.premisave.property.dto.response.UtilityBillResponse;
import com.premisave.property.entity.MeterReading;
import com.premisave.property.entity.OccupancyHistory;
import com.premisave.property.entity.Property;
import com.premisave.property.entity.RentalUnit;
import com.premisave.property.entity.Tenant;
import com.premisave.property.entity.UtilityBill;
import com.premisave.property.enums.MeterType;
import com.premisave.property.enums.PaymentStatus;
import com.premisave.property.enums.UtilityType;
import com.premisave.property.exception.BadRequestException;
import com.premisave.property.exception.ConflictException;
import com.premisave.property.exception.ResourceNotFoundException;
import com.premisave.property.repository.MeterReadingRepository;
import com.premisave.property.repository.OccupancyHistoryRepository;
import com.premisave.property.repository.PropertyRepository;
import com.premisave.property.repository.RentalUnitRepository;
import com.premisave.property.repository.TenantRepository;
import com.premisave.property.repository.UtilityBillRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UtilityBillingService {

    private final UtilityBillRepository utilityBillRepository;
    private final OccupancyHistoryRepository occupancyHistoryRepository;
    private final MeterReadingRepository meterReadingRepository;
    private final RentalUnitRepository rentalUnitRepository;
    private final TenantRepository tenantRepository;
    private final PropertyRepository propertyRepository;
    private final UtilityRatesProperties utilityRatesProperties;

    @Transactional
    public UtilityBillResponse generateBill(UtilityBillRequest request) {
        String tenantId = resolveCurrentTenant(request.getRentalUnitId());

        assertNoOverlappingBill(request.getRentalUnitId(), request.getUtilityType(),
                request.getBillingPeriodStart(), request.getBillingPeriodEnd());

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
        if (utilityBillRepository.existsBySourceMeterReadingId(request.getMeterReadingId())) {
            throw new ConflictException(
                    "A utility bill has already been generated from this meter reading");
        }

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
        bill.setSourceMeterReadingId(reading.getId());

        return toResponse(utilityBillRepository.save(bill));
    }

    @Transactional
    public UtilityBillResponse payBill(PayUtilityBillRequest request) {
        UtilityBill bill = findOrThrow(request.getBillId());

        if (bill.getStatus() == PaymentStatus.PAID || bill.getStatus() == PaymentStatus.OVERPAID) {
            throw new ConflictException(
                    "This bill has already been fully settled (" + bill.getStatus()
                            + ") and cannot accept further payments");
        }

        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Payment amount must be greater than zero");
        }

        BigDecimal newAmountPaid = bill.getAmountPaid().add(request.getAmount());
        int comparison = newAmountPaid.compareTo(bill.getAmount());

        if (comparison == 0) {
            bill.setStatus(PaymentStatus.PAID);
        } else if (comparison > 0) {
            bill.setStatus(PaymentStatus.OVERPAID);
        } else {
            bill.setStatus(PaymentStatus.PARTIALLY_PAID);
        }

        bill.setAmountPaid(newAmountPaid);

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

    // ------------------------------------------------------------------
    // Duplicate-bill prevention
    // ------------------------------------------------------------------

    /**
     * Rejects a manually-generated bill whose billing period overlaps an
     * existing bill for the same unit and utility type. A missing start or
     * end bound is treated as open-ended in that direction, so a bill with
     * no period set (covers "everything") blocks any other bill for that
     * unit/utility type until it's resolved.
     */
    private void assertNoOverlappingBill(String rentalUnitId, UtilityType utilityType,
                                          LocalDateTime periodStart, LocalDateTime periodEnd) {
        List<UtilityBill> existingBills =
                utilityBillRepository.findByRentalUnitIdAndUtilityType(rentalUnitId, utilityType);

        for (UtilityBill existing : existingBills) {
            if (periodsOverlap(periodStart, periodEnd,
                    existing.getBillingPeriodStart(), existing.getBillingPeriodEnd())) {
                throw new ConflictException(
                        "A " + utilityType + " bill already exists for this unit covering an overlapping "
                                + "billing period (existing bill id: " + existing.getId() + ")");
            }
        }
    }

    private boolean periodsOverlap(LocalDateTime aStart, LocalDateTime aEnd,
                                    LocalDateTime bStart, LocalDateTime bEnd) {
        LocalDateTime aStartEff = aStart != null ? aStart : LocalDateTime.MIN;
        LocalDateTime aEndEff = aEnd != null ? aEnd : LocalDateTime.MAX;
        LocalDateTime bStartEff = bStart != null ? bStart : LocalDateTime.MIN;
        LocalDateTime bEndEff = bEnd != null ? bEnd : LocalDateTime.MAX;

        return !aEndEff.isBefore(bStartEff) && !bEndEff.isBefore(aStartEff);
    }

    // ------------------------------------------------------------------

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

    // ------------------------------------------------------------------
    // Mapping
    // ------------------------------------------------------------------

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
        response.setSourceMeterReadingId(bill.getSourceMeterReadingId());

        applyPaymentSummary(response, bill);

        if (bill.getTenantId() != null) {
            tenantRepository.findById(bill.getTenantId())
                    .ifPresent(tenant -> response.setTenant(toTenantSummary(tenant)));
        }

        RentalUnit unit = null;
        if (bill.getRentalUnitId() != null) {
            unit = rentalUnitRepository.findById(bill.getRentalUnitId()).orElse(null);
            if (unit != null) {
                response.setRentalUnit(toRentalUnitSummary(unit));
            }
        }

        if (unit != null && unit.getPropertyId() != null) {
            propertyRepository.findById(unit.getPropertyId())
                    .ifPresent(property -> response.setProperty(toPropertySummary(property)));
        }

        return response;
    }

    /**
     * Computes balanceDue / overpaidAmount / a human-readable paymentMessage
     * from amount, amountPaid, and status — always accurate on any read,
     * not just right after payBill() runs.
     */
    private void applyPaymentSummary(UtilityBillResponse response, UtilityBill bill) {
        BigDecimal amount = bill.getAmount() != null ? bill.getAmount() : BigDecimal.ZERO;
        BigDecimal amountPaid = bill.getAmountPaid() != null ? bill.getAmountPaid() : BigDecimal.ZERO;
        BigDecimal difference = amountPaid.subtract(amount); // positive = overpaid, negative = balance owed

        BigDecimal balanceDue = difference.compareTo(BigDecimal.ZERO) < 0
                ? difference.negate()
                : BigDecimal.ZERO;
        BigDecimal overpaidAmount = difference.compareTo(BigDecimal.ZERO) > 0
                ? difference
                : BigDecimal.ZERO;

        response.setBalanceDue(balanceDue);
        response.setOverpaidAmount(overpaidAmount);
        response.setPaymentMessage(buildPaymentMessage(bill.getStatus(), amount, amountPaid, balanceDue, overpaidAmount));
    }

    private String buildPaymentMessage(PaymentStatus status, BigDecimal amount, BigDecimal amountPaid,
                                        BigDecimal balanceDue, BigDecimal overpaidAmount) {
        return switch (status) {
            case PAID -> "This bill has been paid in full. Thank you!";
            case OVERPAID -> "Payment received in full, with an overpayment of KES " + overpaidAmount
                    + " credited above the amount due. Please contact your property owner regarding a refund "
                    + "or credit toward a future bill.";
            case PARTIALLY_PAID -> "Partial payment received. KES " + balanceDue + " is still outstanding "
                    + "out of KES " + amount + ".";
            case OVERDUE -> "This bill is overdue. KES " + balanceDue + " is outstanding.";
            case PENDING -> amountPaid.compareTo(BigDecimal.ZERO) > 0
                    ? "Payment is being processed."
                    : "No payment has been made yet. KES " + amount + " is due.";
            case FAILED -> "The last payment attempt on this bill failed. Please try again.";
            case REFUNDED -> "This bill has been refunded.";
        };
    }

    private TenantSummaryResponse toTenantSummary(Tenant tenant) {
        TenantSummaryResponse summary = new TenantSummaryResponse();
        summary.setId(tenant.getId());
        summary.setFullName(tenant.getFullName());
        summary.setPhoneNumber(tenant.getPhoneNumber());
        summary.setEmail(tenant.getEmail());
        return summary;
    }

    private RentalUnitSummaryResponse toRentalUnitSummary(RentalUnit unit) {
        RentalUnitSummaryResponse summary = new RentalUnitSummaryResponse();
        summary.setId(unit.getId());
        summary.setUnitNumber(unit.getUnitNumber());
        summary.setFloor(unit.getFloor());
        summary.setRentAmount(unit.getRentAmount());
        summary.setStatus(unit.getStatus());
        return summary;
    }

    private PropertySummaryResponse toPropertySummary(Property property) {
        PropertySummaryResponse summary = new PropertySummaryResponse();
        summary.setId(property.getId());
        summary.setTitle(property.getTitle());
        summary.setPropertyType(property.getPropertyType());
        summary.setRegistrationNumber(property.getRegistrationNumber());
        return summary;
    }
}