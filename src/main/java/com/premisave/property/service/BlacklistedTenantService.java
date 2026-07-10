package com.premisave.property.service;

import com.premisave.property.dto.request.BlacklistTenantRequest;
import com.premisave.property.dto.response.BlacklistedTenantResponse;
import com.premisave.property.dto.response.AddressResponse;
import com.premisave.property.dto.response.PropertySummaryResponse;
import com.premisave.property.dto.response.RentalUnitSummaryResponse;
import com.premisave.property.entity.Address;
import com.premisave.property.entity.BlacklistedTenant;
import com.premisave.property.entity.OccupancyHistory;
import com.premisave.property.entity.Property;
import com.premisave.property.entity.RentalUnit;
import com.premisave.property.exception.ConflictException;
import com.premisave.property.exception.ResourceNotFoundException;
import com.premisave.property.repository.BlacklistedTenantRepository;
import com.premisave.property.repository.OccupancyHistoryRepository;
import com.premisave.property.repository.PropertyRepository;
import com.premisave.property.repository.RentalUnitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BlacklistedTenantService {

    private final BlacklistedTenantRepository blacklistedTenantRepository;
    private final TenantService tenantService;
    private final OccupancyHistoryRepository occupancyHistoryRepository;
    private final RentalUnitRepository rentalUnitRepository;
    private final PropertyRepository propertyRepository;

    @Transactional
    public BlacklistedTenantResponse blacklistTenant(BlacklistTenantRequest request) {
        blacklistedTenantRepository.findByTenantId(request.getTenantId()).ifPresent(existing -> {
            throw new ConflictException("Tenant is already blacklisted");
        });

        BlacklistedTenant blacklisted = new BlacklistedTenant();
        blacklisted.setTenantId(request.getTenantId());
        blacklisted.setReason(request.getReason());
        blacklisted.setNotes(request.getNotes());
        blacklisted.setBlacklistedAt(LocalDateTime.now());

        // Capture the most recent occupancy record for this tenant, current
        // or past, so the blacklist entry shows which unit/property this
        // relates to — even if the tenant has since moved out.
        findMostRecentOccupancy(request.getTenantId()).ifPresent(occupancy -> {
            blacklisted.setRentalUnitId(occupancy.getRentalUnitId());
            blacklisted.setPropertyId(occupancy.getPropertyId());
        });

        BlacklistedTenant saved = blacklistedTenantRepository.save(blacklisted);

        tenantService.setBlacklisted(request.getTenantId(), true);

        return toResponse(saved);
    }

    @Transactional
    public void removeFromBlacklist(String tenantId) {
        BlacklistedTenant blacklisted = blacklistedTenantRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant is not currently blacklisted"));

        blacklistedTenantRepository.delete(blacklisted);
        tenantService.setBlacklisted(tenantId, false);
    }

    public BlacklistedTenantResponse getBlacklistEntry(String tenantId) {
        return blacklistedTenantRepository.findByTenantId(tenantId)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant is not currently blacklisted"));
    }

    public List<BlacklistedTenantResponse> getAllBlacklisted() {
        return blacklistedTenantRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    private Optional<OccupancyHistory> findMostRecentOccupancy(String tenantId) {
        return occupancyHistoryRepository.findByTenantId(tenantId).stream()
                .max(Comparator.comparing(OccupancyHistory::getMoveInDate));
    }

    private BlacklistedTenantResponse toResponse(BlacklistedTenant blacklisted) {
        BlacklistedTenantResponse response = new BlacklistedTenantResponse();
        response.setTenantId(blacklisted.getTenantId());
        response.setReason(blacklisted.getReason());
        response.setBlacklistedAt(blacklisted.getBlacklistedAt());

        if (blacklisted.getRentalUnitId() != null) {
            rentalUnitRepository.findById(blacklisted.getRentalUnitId())
                    .ifPresent(unit -> response.setRentalUnit(toRentalUnitSummary(unit)));
        }

        if (blacklisted.getPropertyId() != null) {
            propertyRepository.findById(blacklisted.getPropertyId())
                    .ifPresent(property -> response.setProperty(toPropertySummary(property)));
        }

        return response;
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
        summary.setAddress(toAddressResponse(property.getAddress()));
        summary.setRegistrationNumber(property.getRegistrationNumber());
        return summary;
    }

    private AddressResponse toAddressResponse(Address address) {
        if (address == null) {
            return null;
        }
        AddressResponse response = new AddressResponse();
        response.setStreet(address.getStreet());
        response.setCity(address.getCity());
        response.setState(address.getState());
        response.setCountry(address.getCountry());
        response.setPostalCode(address.getPostalCode());
        response.setLandmark(address.getLandmark());
        return response;
    }
}