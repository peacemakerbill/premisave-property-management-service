package com.premisave.property.service;

import com.premisave.property.dto.response.OccupancyReportResponse;
import com.premisave.property.dto.response.OwnerSummaryResponse;
import com.premisave.property.dto.response.PropertyOccupancyResponse;
import com.premisave.property.dto.response.PropertyRevenueResponse;
import com.premisave.property.dto.response.PropertySummaryResponse;
import com.premisave.property.dto.response.RevenueReportResponse;
import com.premisave.property.entity.Lease;
import com.premisave.property.entity.LeaseRentUnitPayment;
import com.premisave.property.entity.Owner;
import com.premisave.property.entity.Property;
import com.premisave.property.entity.RentalUnit;
import com.premisave.property.enums.PaymentStatus;
import com.premisave.property.enums.UnitStatus;
import com.premisave.property.repository.LeaseRentUnitPaymentRepository;
import com.premisave.property.repository.LeaseRepository;
import com.premisave.property.repository.OwnerRepository;
import com.premisave.property.repository.PropertyRepository;
import com.premisave.property.repository.RentalUnitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final PropertyRepository propertyRepository;
    private final RentalUnitRepository rentalUnitRepository;
    private final LeaseRepository leaseRepository;
    private final LeaseRentUnitPaymentRepository leaseRentUnitPaymentRepository;
    private final OwnerRepository ownerRepository;

    public OccupancyReportResponse getOccupancyReport(String ownerId) {
        List<Property> properties = propertyRepository.findByOwnerId(ownerId);

        long total = 0;
        long occupied = 0;
        List<PropertyOccupancyResponse> breakdown = new ArrayList<>();

        for (Property property : properties) {
            List<RentalUnit> units = rentalUnitRepository.findByPropertyId(property.getId());
            long propertyTotal = units.size();
            long propertyOccupied = units.stream().filter(u -> u.getStatus() == UnitStatus.OCCUPIED).count();

            total += propertyTotal;
            occupied += propertyOccupied;

            PropertyOccupancyResponse propertyBreakdown = new PropertyOccupancyResponse();
            propertyBreakdown.setProperty(toPropertySummary(property));
            propertyBreakdown.setTotalUnits(propertyTotal);
            propertyBreakdown.setOccupiedUnits(propertyOccupied);
            propertyBreakdown.setVacantUnits(propertyTotal - propertyOccupied);
            propertyBreakdown.setOccupancyRate(
                    propertyTotal == 0 ? 0.0 : (double) propertyOccupied / propertyTotal * 100);
            breakdown.add(propertyBreakdown);
        }

        OccupancyReportResponse response = new OccupancyReportResponse();
        response.setOwnerId(ownerId);
        response.setTotalUnits(total);
        response.setOccupiedUnits(occupied);
        response.setVacantUnits(total - occupied);
        response.setOccupancyRate(total == 0 ? 0.0 : (double) occupied / total * 100);
        response.setProperties(breakdown);

        ownerRepository.findById(ownerId).ifPresent(owner -> response.setOwner(toOwnerSummary(owner)));

        return response;
    }

    public RevenueReportResponse getRevenueReport(String ownerId, LocalDate start, LocalDate end) {
        LocalDateTime startDateTime = start.atStartOfDay();
        LocalDateTime endDateTime = end.plusDays(1).atStartOfDay();

        List<Property> properties = propertyRepository.findByOwnerId(ownerId);

        BigDecimal totalRevenue = BigDecimal.ZERO;
        int totalPaymentCount = 0;
        List<PropertyRevenueResponse> breakdown = new ArrayList<>();

        for (Property property : properties) {
            List<String> leaseIds = leaseRepository.findByPropertyIdIn(List.of(property.getId())).stream()
                    .map(Lease::getId)
                    .toList();

            List<LeaseRentUnitPayment> payments = leaseIds.isEmpty()
                    ? List.of()
                    : leaseRentUnitPaymentRepository.findByLeaseIdInAndPaidAtBetween(
                            leaseIds, startDateTime, endDateTime);

            // OVERPAID is included here because it represents real money
            // already collected (in fact, more than what was strictly due)
            // — it should never be excluded from actual revenue collected
            // in this period.
            BigDecimal propertyRevenue = payments.stream()
                    .filter(p -> p.getStatus() == PaymentStatus.PAID
                            || p.getStatus() == PaymentStatus.PARTIALLY_PAID
                            || p.getStatus() == PaymentStatus.OVERPAID)
                    .map(LeaseRentUnitPayment::getAmountPaid)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            totalRevenue = totalRevenue.add(propertyRevenue);
            totalPaymentCount += payments.size();

            PropertyRevenueResponse propertyBreakdown = new PropertyRevenueResponse();
            propertyBreakdown.setProperty(toPropertySummary(property));
            propertyBreakdown.setTotalRevenue(propertyRevenue);
            propertyBreakdown.setPaymentCount(payments.size());
            breakdown.add(propertyBreakdown);
        }

        RevenueReportResponse response = new RevenueReportResponse();
        response.setOwnerId(ownerId);
        response.setPeriodStart(start);
        response.setPeriodEnd(end);
        response.setTotalRevenue(totalRevenue);
        response.setPaymentCount(totalPaymentCount);
        response.setProperties(breakdown);

        ownerRepository.findById(ownerId).ifPresent(owner -> response.setOwner(toOwnerSummary(owner)));

        return response;
    }

    private OwnerSummaryResponse toOwnerSummary(Owner owner) {
        OwnerSummaryResponse summary = new OwnerSummaryResponse();
        summary.setId(owner.getId());
        summary.setFullName(owner.getFullName());
        summary.setPhoneNumber(owner.getPhoneNumber());
        summary.setEmail(owner.getEmail());
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