package com.premisave.property.service;

import com.premisave.property.dto.response.DashboardSummaryResponse;
import com.premisave.property.entity.Property;
import com.premisave.property.entity.RentPayment;
import com.premisave.property.entity.RentalUnit;
import com.premisave.property.enums.LeaseStatus;
import com.premisave.property.enums.MaintenanceStatus;
import com.premisave.property.enums.PaymentStatus;
import com.premisave.property.enums.UnitStatus;
import com.premisave.property.repository.LeaseRepository;
import com.premisave.property.repository.MaintenanceRequestRepository;
import com.premisave.property.repository.PropertyRepository;
import com.premisave.property.repository.RentPaymentRepository;
import com.premisave.property.repository.RentalUnitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final PropertyRepository propertyRepository;
    private final RentalUnitRepository rentalUnitRepository;
    private final LeaseRepository leaseRepository;
    private final MaintenanceRequestRepository maintenanceRepository;
    private final RentPaymentRepository rentPaymentRepository;

    public DashboardSummaryResponse getOwnerDashboard(String ownerId) {
        List<String> propertyIds = propertyRepository.findByOwnerId(ownerId).stream()
                .map(Property::getId)
                .toList();

        long totalProperties = propertyIds.size();
        long totalUnits = propertyIds.isEmpty() ? 0 : rentalUnitRepository.countByPropertyIdIn(propertyIds);
        long occupiedUnits = propertyIds.isEmpty() ? 0
                : rentalUnitRepository.countByPropertyIdInAndStatus(propertyIds, UnitStatus.OCCUPIED);
        long activeLeases = propertyIds.isEmpty() ? 0
                : leaseRepository.countByPropertyIdInAndStatus(propertyIds, LeaseStatus.ACTIVE);
        long pendingMaintenance = countPendingMaintenance(propertyIds);

        DashboardSummaryResponse summary = new DashboardSummaryResponse();
        summary.setTotalProperties(totalProperties);
        summary.setTotalUnits(totalUnits);
        summary.setOccupiedUnits(occupiedUnits);
        summary.setActiveLeases(activeLeases);
        summary.setMonthlyRevenue(calculateMonthlyRevenue(propertyIds));
        summary.setPendingMaintenance(pendingMaintenance);

        return summary;
    }

    private long countPendingMaintenance(List<String> propertyIds) {
        if (propertyIds.isEmpty()) {
            return 0;
        }
        List<String> unitIds = propertyIds.stream()
                .flatMap(propertyId -> rentalUnitRepository.findByPropertyId(propertyId).stream())
                .map(RentalUnit::getId)
                .toList();

        if (unitIds.isEmpty()) {
            return 0;
        }

        return maintenanceRepository.findByRentalUnitIdInAndStatusIn(unitIds,
                List.of(MaintenanceStatus.PENDING, MaintenanceStatus.IN_PROGRESS)).size();
    }

    private BigDecimal calculateMonthlyRevenue(List<String> propertyIds) {
        if (propertyIds.isEmpty()) {
            return BigDecimal.ZERO;
        }

        YearMonth currentMonth = YearMonth.now();
        LocalDateTime start = currentMonth.atDay(1).atStartOfDay();
        LocalDateTime end = currentMonth.plusMonths(1).atDay(1).atStartOfDay();

        List<RentPayment> payments = rentPaymentRepository
                .findByPropertyIdInAndPaidAtBetween(propertyIds, start, end);

        return payments.stream()
                .filter(p -> p.getStatus() == PaymentStatus.PAID || p.getStatus() == PaymentStatus.PARTIALLY_PAID)
                .map(RentPayment::getAmountPaid)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}