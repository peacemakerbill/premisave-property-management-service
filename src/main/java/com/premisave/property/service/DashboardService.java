package com.premisave.property.service;

import com.premisave.property.dto.response.DashboardSummaryResponse;
import com.premisave.property.enums.LeaseStatus;
import com.premisave.property.enums.UnitStatus;
import com.premisave.property.repository.PropertyRepository;
import com.premisave.property.repository.RentalUnitRepository;
import com.premisave.property.repository.LeaseRepository;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final PropertyRepository propertyRepository;
    private final RentalUnitRepository rentalUnitRepository;
    private final LeaseRepository leaseRepository;

    public DashboardSummaryResponse getOwnerDashboard(String ownerId) {
        long totalProperties = propertyRepository.findByOwnerId(ownerId).size();
        long occupiedUnits = rentalUnitRepository.countByStatus(UnitStatus.OCCUPIED); // adjust query
        long activeLeases = leaseRepository.findByStatus(LeaseStatus.ACTIVE).size();

        DashboardSummaryResponse summary = new DashboardSummaryResponse();
        summary.setTotalProperties(totalProperties);
        summary.setOccupiedUnits(occupiedUnits);
        summary.setMonthlyRevenue(calculateMonthlyRevenue(ownerId));
        summary.setPendingMaintenance(5); // TODO: from maintenance repo

        return summary;
    }

    private BigDecimal calculateMonthlyRevenue(String ownerId) {
        // Implement revenue calculation logic
        return BigDecimal.valueOf(125000);
    }
}