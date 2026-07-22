package com.premisave.property.service;

import com.premisave.property.dto.response.ActiveLeaseSummaryResponse;
import com.premisave.property.dto.response.DashboardSummaryResponse;
import com.premisave.property.dto.response.LeaseSummaryResponse;
import com.premisave.property.dto.response.MaintenanceSummaryResponse;
import com.premisave.property.dto.response.OwnerSummaryResponse;
import com.premisave.property.dto.response.PropertyOccupancyResponse;
import com.premisave.property.dto.response.PropertyRevenueResponse;
import com.premisave.property.dto.response.PropertySummaryResponse;
import com.premisave.property.dto.response.RentalUnitSummaryResponse;
import com.premisave.property.dto.response.TenantSummaryResponse;
import com.premisave.property.entity.Lease;
import com.premisave.property.entity.LeaseRentUnitPayment;
import com.premisave.property.entity.Maintenance;
import com.premisave.property.entity.Owner;
import com.premisave.property.entity.Property;
import com.premisave.property.entity.RentalUnit;
import com.premisave.property.entity.Tenant;
import com.premisave.property.enums.LeaseStatus;
import com.premisave.property.enums.MaintenanceStatus;
import com.premisave.property.enums.PaymentStatus;
import com.premisave.property.enums.UnitStatus;
import com.premisave.property.repository.LeaseRentUnitPaymentRepository;
import com.premisave.property.repository.LeaseRepository;
import com.premisave.property.repository.MaintenanceRequestRepository;
import com.premisave.property.repository.OwnerRepository;
import com.premisave.property.repository.PropertyRepository;
import com.premisave.property.repository.RentalUnitRepository;
import com.premisave.property.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final PropertyRepository propertyRepository;
    private final RentalUnitRepository rentalUnitRepository;
    private final LeaseRepository leaseRepository;
    private final MaintenanceRequestRepository maintenanceRepository;
    private final LeaseRentUnitPaymentRepository leaseRentUnitPaymentRepository;
    private final OwnerRepository ownerRepository;
    private final TenantRepository tenantRepository;

    public DashboardSummaryResponse getOwnerDashboard(String ownerId) {
        List<Property> properties = propertyRepository.findByOwnerId(ownerId);
        List<String> propertyIds = properties.stream().map(Property::getId).toList();
        Map<String, Property> propertiesById = new HashMap<>();
        properties.forEach(property -> propertiesById.put(property.getId(), property));

        List<PropertyOccupancyResponse> occupancyBreakdown = buildOccupancyBreakdown(properties);
        long totalUnits = occupancyBreakdown.stream().mapToLong(PropertyOccupancyResponse::getTotalUnits).sum();
        long occupiedUnits = occupancyBreakdown.stream().mapToLong(PropertyOccupancyResponse::getOccupiedUnits).sum();

        List<Lease> activeLeases = propertyIds.isEmpty()
                ? List.of()
                : leaseRepository.findByPropertyIdIn(propertyIds).stream()
                        .filter(lease -> lease.getStatus() == LeaseStatus.ACTIVE)
                        .toList();
        List<ActiveLeaseSummaryResponse> activeLeaseDetails = buildActiveLeaseDetails(activeLeases, propertiesById);

        List<PropertyRevenueResponse> revenueBreakdown = buildRevenueBreakdown(properties);
        BigDecimal monthlyRevenue = revenueBreakdown.stream()
                .map(PropertyRevenueResponse::getTotalRevenue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<MaintenanceSummaryResponse> maintenanceBreakdown = buildMaintenanceBreakdown(propertyIds, propertiesById);
        long pendingMaintenance = maintenanceBreakdown.size();

        DashboardSummaryResponse summary = new DashboardSummaryResponse();
        summary.setTotalProperties(properties.size());
        summary.setTotalUnits(totalUnits);
        summary.setOccupiedUnits(occupiedUnits);
        summary.setActiveLeases(activeLeases.size());
        summary.setMonthlyRevenue(monthlyRevenue);
        summary.setPendingMaintenance(pendingMaintenance);

        summary.setProperties(occupancyBreakdown);
        summary.setActiveLeaseDetails(activeLeaseDetails);
        summary.setRevenueBreakdown(revenueBreakdown);
        summary.setPendingMaintenanceRequests(maintenanceBreakdown);

        ownerRepository.findById(ownerId).ifPresent(owner -> summary.setOwner(toOwnerSummary(owner)));

        return summary;
    }

    private List<PropertyOccupancyResponse> buildOccupancyBreakdown(List<Property> properties) {
        List<PropertyOccupancyResponse> breakdown = new ArrayList<>();
        for (Property property : properties) {
            List<RentalUnit> units = rentalUnitRepository.findByPropertyId(property.getId());
            long propertyTotal = units.size();
            long propertyOccupied = units.stream().filter(u -> u.getStatus() == UnitStatus.OCCUPIED).count();

            PropertyOccupancyResponse entry = new PropertyOccupancyResponse();
            entry.setProperty(toPropertySummary(property));
            entry.setTotalUnits(propertyTotal);
            entry.setOccupiedUnits(propertyOccupied);
            entry.setVacantUnits(propertyTotal - propertyOccupied);
            entry.setOccupancyRate(propertyTotal == 0 ? 0.0 : (double) propertyOccupied / propertyTotal * 100);
            breakdown.add(entry);
        }
        return breakdown;
    }

    private List<ActiveLeaseSummaryResponse> buildActiveLeaseDetails(List<Lease> activeLeases,
                                                                       Map<String, Property> propertiesById) {
        List<ActiveLeaseSummaryResponse> details = new ArrayList<>();
        for (Lease lease : activeLeases) {
            ActiveLeaseSummaryResponse entry = new ActiveLeaseSummaryResponse();
            entry.setLease(toLeaseSummary(lease));

            if (lease.getTenantId() != null) {
                tenantRepository.findById(lease.getTenantId()).ifPresent(tenant -> entry.setTenant(toTenantSummary(tenant)));
            }

            Property property = propertiesById.get(lease.getPropertyId());
            if (property != null) {
                entry.setProperty(toPropertySummary(property));
            }

            if (lease.getRentalUnitId() != null) {
                rentalUnitRepository.findById(lease.getRentalUnitId())
                        .ifPresent(unit -> entry.setUnit(toRentalUnitSummary(unit)));
            }

            details.add(entry);
        }
        return details;
    }

    private List<PropertyRevenueResponse> buildRevenueBreakdown(List<Property> properties) {
        YearMonth currentMonth = YearMonth.now();
        LocalDateTime start = currentMonth.atDay(1).atStartOfDay();
        LocalDateTime end = currentMonth.plusMonths(1).atDay(1).atStartOfDay();

        List<PropertyRevenueResponse> breakdown = new ArrayList<>();
        for (Property property : properties) {
            List<String> leaseIds = leaseRepository.findByPropertyIdIn(List.of(property.getId())).stream()
                    .map(Lease::getId)
                    .toList();

            List<LeaseRentUnitPayment> payments = leaseIds.isEmpty()
                    ? List.of()
                    : leaseRentUnitPaymentRepository.findByLeaseIdInAndPaidAtBetween(leaseIds, start, end);

            // OVERPAID is included here because it represents real money
            // already collected (in fact, more than what was strictly due)
            // — it should never be excluded from actual revenue collected
            // this month.
            BigDecimal propertyRevenue = payments.stream()
                    .filter(p -> p.getStatus() == PaymentStatus.PAID
                            || p.getStatus() == PaymentStatus.PARTIALLY_PAID
                            || p.getStatus() == PaymentStatus.OVERPAID)
                    .map(LeaseRentUnitPayment::getAmountPaid)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            PropertyRevenueResponse entry = new PropertyRevenueResponse();
            entry.setProperty(toPropertySummary(property));
            entry.setTotalRevenue(propertyRevenue);
            entry.setPaymentCount(payments.size());
            breakdown.add(entry);
        }
        return breakdown;
    }

    private List<MaintenanceSummaryResponse> buildMaintenanceBreakdown(List<String> propertyIds,
                                                                         Map<String, Property> propertiesById) {
        if (propertyIds.isEmpty()) {
            return List.of();
        }

        List<RentalUnit> units = propertyIds.stream()
                .flatMap(propertyId -> rentalUnitRepository.findByPropertyId(propertyId).stream())
                .toList();
        Map<String, RentalUnit> unitsById = new HashMap<>();
        units.forEach(unit -> unitsById.put(unit.getId(), unit));

        List<String> unitIds = units.stream().map(RentalUnit::getId).toList();
        if (unitIds.isEmpty()) {
            return List.of();
        }

        List<Maintenance> requests = maintenanceRepository.findByRentalUnitIdInAndStatusIn(unitIds,
                List.of(MaintenanceStatus.PENDING, MaintenanceStatus.IN_PROGRESS));

        List<MaintenanceSummaryResponse> breakdown = new ArrayList<>();
        for (Maintenance request : requests) {
            MaintenanceSummaryResponse entry = new MaintenanceSummaryResponse();
            entry.setId(request.getId());
            entry.setTitle(request.getTitle());
            entry.setDescription(request.getDescription());
            entry.setStatus(request.getStatus());
            entry.setCreatedAt(request.getCreatedAt());

            if (request.getTenantId() != null) {
                tenantRepository.findById(request.getTenantId())
                        .ifPresent(tenant -> entry.setTenant(toTenantSummary(tenant)));
            }

            RentalUnit unit = unitsById.get(request.getRentalUnitId());
            if (unit != null) {
                entry.setUnit(toRentalUnitSummary(unit));
                Property property = propertiesById.get(unit.getPropertyId());
                if (property != null) {
                    entry.setProperty(toPropertySummary(property));
                }
            }

            breakdown.add(entry);
        }
        return breakdown;
    }

    private OwnerSummaryResponse toOwnerSummary(Owner owner) {
        OwnerSummaryResponse summary = new OwnerSummaryResponse();
        summary.setId(owner.getId());
        summary.setFullName(owner.getFullName());
        summary.setPhoneNumber(owner.getPhoneNumber());
        summary.setEmail(owner.getEmail());
        return summary;
    }

    private TenantSummaryResponse toTenantSummary(Tenant tenant) {
        TenantSummaryResponse summary = new TenantSummaryResponse();
        summary.setId(tenant.getId());
        summary.setFullName(tenant.getFullName());
        summary.setPhoneNumber(tenant.getPhoneNumber());
        summary.setEmail(tenant.getEmail());
        return summary;
    }

    private LeaseSummaryResponse toLeaseSummary(Lease lease) {
        LeaseSummaryResponse summary = new LeaseSummaryResponse();
        summary.setId(lease.getId());
        summary.setLeaseType(lease.getLeaseType());
        summary.setStartDate(lease.getStartDate());
        summary.setEndDate(lease.getEndDate());
        summary.setMonthlyRent(lease.getMonthlyRent());
        summary.setStatus(lease.getStatus());
        return summary;
    }

    private PropertySummaryResponse toPropertySummary(Property property) {
        PropertySummaryResponse summary = new PropertySummaryResponse();
        summary.setId(property.getId());
        summary.setTitle(property.getTitle());
        summary.setPropertyType(property.getPropertyType());
        summary.setAddress(toAddressResponse(property.getAddress()));
        summary.setRegistrationNumber(property.getRegistrationNumber());
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

    private com.premisave.property.dto.response.AddressResponse toAddressResponse(
            com.premisave.property.entity.Address address) {
        if (address == null) {
            return null;
        }
        com.premisave.property.dto.response.AddressResponse response =
                new com.premisave.property.dto.response.AddressResponse();
        response.setStreet(address.getStreet());
        response.setCity(address.getCity());
        response.setState(address.getState());
        response.setCountry(address.getCountry());
        response.setPostalCode(address.getPostalCode());
        response.setLandmark(address.getLandmark());
        return response;
    }
}