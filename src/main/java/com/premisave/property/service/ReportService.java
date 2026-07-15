package com.premisave.property.service;

import com.premisave.property.dto.response.OccupancyReportResponse;
import com.premisave.property.dto.response.RevenueReportResponse;
import com.premisave.property.entity.Lease;
import com.premisave.property.entity.LeaseRentPayment;
import com.premisave.property.entity.Property;
import com.premisave.property.entity.RentalUnit;
import com.premisave.property.enums.PaymentStatus;
import com.premisave.property.enums.UnitStatus;
import com.premisave.property.repository.LeaseRentPaymentRepository;
import com.premisave.property.repository.LeaseRepository;
import com.premisave.property.repository.PropertyRepository;
import com.premisave.property.repository.RentalUnitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final PropertyRepository propertyRepository;
    private final RentalUnitRepository rentalUnitRepository;
    private final LeaseRepository leaseRepository;
    private final LeaseRentPaymentRepository leaseRentPaymentRepository;

    public OccupancyReportResponse getOccupancyReport(String ownerId) {
        List<RentalUnit> units = getOwnerUnits(ownerId);

        long total = units.size();
        long occupied = units.stream().filter(u -> u.getStatus() == UnitStatus.OCCUPIED).count();

        OccupancyReportResponse response = new OccupancyReportResponse();
        response.setOwnerId(ownerId);
        response.setTotalUnits(total);
        response.setOccupiedUnits(occupied);
        response.setVacantUnits(total - occupied);
        response.setOccupancyRate(total == 0 ? 0.0 : (double) occupied / total * 100);

        return response;
    }

    public RevenueReportResponse getRevenueReport(String ownerId, LocalDate start, LocalDate end) {
        LocalDateTime startDateTime = start.atStartOfDay();
        LocalDateTime endDateTime = end.plusDays(1).atStartOfDay();

        List<String> propertyIds = propertyRepository.findByOwnerId(ownerId).stream()
                .map(Property::getId)
                .toList();

        List<String> leaseIds = propertyIds.isEmpty()
                ? List.of()
                : leaseRepository.findByPropertyIdIn(propertyIds).stream()
                        .map(Lease::getId)
                        .toList();

        List<LeaseRentPayment> payments = leaseIds.isEmpty()
                ? List.of()
                : leaseRentPaymentRepository.findByLeaseIdInAndPaidAtBetween(leaseIds, startDateTime, endDateTime);

        // OVERPAID is included here because it represents real money already
        // collected (in fact, more than what was strictly due) — it should
        // never be excluded from actual revenue collected in this period.
        BigDecimal totalRevenue = payments.stream()
                .filter(p -> p.getStatus() == PaymentStatus.PAID
                        || p.getStatus() == PaymentStatus.PARTIALLY_PAID
                        || p.getStatus() == PaymentStatus.OVERPAID)
                .map(LeaseRentPayment::getAmountPaid)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        RevenueReportResponse response = new RevenueReportResponse();
        response.setOwnerId(ownerId);
        response.setPeriodStart(start);
        response.setPeriodEnd(end);
        response.setTotalRevenue(totalRevenue);
        response.setPaymentCount(payments.size());

        return response;
    }

    private List<RentalUnit> getOwnerUnits(String ownerId) {
        return propertyRepository.findByOwnerId(ownerId).stream()
                .map(Property::getId)
                .flatMap(propertyId -> rentalUnitRepository.findByPropertyId(propertyId).stream())
                .toList();
    }
}