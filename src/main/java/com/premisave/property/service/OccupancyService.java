package com.premisave.property.service;

import com.premisave.property.dto.response.AddressResponse;
import com.premisave.property.dto.response.OccupancyResponse;
import com.premisave.property.dto.response.PropertySummaryResponse;
import com.premisave.property.dto.response.RentalUnitSummaryResponse;
import com.premisave.property.dto.response.TenantSummaryResponse;
import com.premisave.property.entity.Address;
import com.premisave.property.entity.Lease;
import com.premisave.property.entity.OccupancyHistory;
import com.premisave.property.entity.Property;
import com.premisave.property.entity.RentalUnit;
import com.premisave.property.entity.Tenant;
import com.premisave.property.enums.LeaseStatus;
import com.premisave.property.enums.LeaseType;
import com.premisave.property.enums.OccupancyType;
import com.premisave.property.enums.UnitStatus;
import com.premisave.property.exception.BadRequestException;
import com.premisave.property.exception.ConflictException;
import com.premisave.property.exception.ResourceNotFoundException;
import com.premisave.property.repository.LeaseRepository;
import com.premisave.property.repository.OccupancyHistoryRepository;
import com.premisave.property.repository.PropertyRepository;
import com.premisave.property.repository.RentalUnitRepository;
import com.premisave.property.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OccupancyService {

    private final OccupancyHistoryRepository occupancyHistoryRepository;
    private final RentalUnitRepository rentalUnitRepository;
    private final LeaseRepository leaseRepository;
    private final TenantRepository tenantRepository;
    private final PropertyRepository propertyRepository;

    // ------------------------------------------------------------------
    // LEASE-BACKED occupancy
    // ------------------------------------------------------------------

    @Transactional
    public OccupancyResponse recordMoveInViaLease(String leaseId) {
        Lease lease = findLeaseOrThrow(leaseId);

        if (lease.getStatus() != LeaseStatus.ACTIVE) {
            throw new BadRequestException("Cannot record move-in for a lease that is not ACTIVE");
        }

        occupancyHistoryRepository.findByLeaseIdAndMoveOutDateIsNull(leaseId).ifPresent(existing -> {
            throw new ConflictException("A move-in has already been recorded for this lease");
        });

        OccupancyType occupancyType = lease.getLeaseType() == LeaseType.WHOLE_PROPERTY
                ? OccupancyType.WHOLE_PROPERTY
                : OccupancyType.UNIT;

        if (occupancyType == OccupancyType.UNIT) {
            occupancyHistoryRepository.findByRentalUnitIdAndMoveOutDateIsNull(lease.getRentalUnitId())
                    .ifPresent(existing -> {
                        throw new ConflictException("This rental unit is already occupied");
                    });
        } else {
            occupancyHistoryRepository.findByPropertyIdAndOccupancyTypeAndMoveOutDateIsNull(
                            lease.getPropertyId(), OccupancyType.WHOLE_PROPERTY)
                    .ifPresent(existing -> {
                        throw new ConflictException("This property is already occupied under a whole-property lease");
                    });
        }

        OccupancyHistory history = new OccupancyHistory();
        history.setOccupancyType(occupancyType);
        history.setRentalUnitId(lease.getRentalUnitId());
        history.setPropertyId(lease.getPropertyId());
        history.setTenantId(lease.getTenantId());
        history.setLeaseId(leaseId);
        history.setMoveInDate(LocalDateTime.now());

        return toResponse(occupancyHistoryRepository.save(history));
    }

    @Transactional
    public OccupancyResponse recordMoveOutViaLease(String leaseId) {
        OccupancyHistory history = occupancyHistoryRepository.findByLeaseIdAndMoveOutDateIsNull(leaseId)
                .orElseThrow(() -> new ResourceNotFoundException("No active occupancy found for this lease"));

        history.setMoveOutDate(LocalDateTime.now());
        OccupancyHistory saved = occupancyHistoryRepository.save(history);

        if (history.getOccupancyType() == OccupancyType.UNIT && history.getRentalUnitId() != null) {
            rentalUnitRepository.findById(history.getRentalUnitId()).ifPresent(unit -> {
                unit.setStatus(UnitStatus.VACANT);
                rentalUnitRepository.save(unit);
            });
        }

        return toResponse(saved);
    }

    // ------------------------------------------------------------------
    // DIRECT occupancy — no Lease involved
    // ------------------------------------------------------------------

    @Transactional
    public OccupancyResponse recordDirectMoveIn(String rentalUnitId, String tenantId) {
        RentalUnit unit = rentalUnitRepository.findById(rentalUnitId)
                .orElseThrow(() -> new ResourceNotFoundException("Rental unit not found"));

        tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));

        if (unit.getStatus() != UnitStatus.VACANT) {
            throw new ConflictException("This rental unit is not available for move-in");
        }

        occupancyHistoryRepository.findByRentalUnitIdAndMoveOutDateIsNull(rentalUnitId).ifPresent(existing -> {
            throw new ConflictException("This rental unit is already occupied");
        });

        OccupancyHistory history = new OccupancyHistory();
        history.setOccupancyType(OccupancyType.UNIT);
        history.setRentalUnitId(rentalUnitId);
        history.setPropertyId(unit.getPropertyId());
        history.setTenantId(tenantId);
        history.setLeaseId(null);
        history.setMoveInDate(LocalDateTime.now());

        OccupancyHistory saved = occupancyHistoryRepository.save(history);

        unit.setStatus(UnitStatus.OCCUPIED);
        rentalUnitRepository.save(unit);

        return toResponse(saved);
    }

    @Transactional
    public OccupancyResponse recordDirectMoveOut(String rentalUnitId, String tenantId) {
        OccupancyHistory history = occupancyHistoryRepository
                .findByRentalUnitIdAndTenantIdAndMoveOutDateIsNull(rentalUnitId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("No active occupancy found for this tenant and unit"));

        if (history.getLeaseId() != null) {
            throw new BadRequestException(
                    "This occupancy is backed by a lease. Use the lease-based move-out endpoint instead.");
        }

        history.setMoveOutDate(LocalDateTime.now());
        OccupancyHistory saved = occupancyHistoryRepository.save(history);

        rentalUnitRepository.findById(rentalUnitId).ifPresent(unit -> {
            unit.setStatus(UnitStatus.VACANT);
            rentalUnitRepository.save(unit);
        });

        return toResponse(saved);
    }

    // ------------------------------------------------------------------
    // Read endpoints
    // ------------------------------------------------------------------

    public List<OccupancyResponse> getUnitHistory(String rentalUnitId) {
        return occupancyHistoryRepository.findByRentalUnitId(rentalUnitId).stream()
                .map(this::toResponse)
                .toList();
    }

    public List<OccupancyResponse> getPropertyHistory(String propertyId) {
        return occupancyHistoryRepository.findByPropertyId(propertyId).stream()
                .map(this::toResponse)
                .toList();
    }

    public List<OccupancyResponse> getTenantHistory(String tenantId) {
        return occupancyHistoryRepository.findByTenantId(tenantId).stream()
                .map(this::toResponse)
                .toList();
    }

    public OccupancyResponse getCurrentOccupancyForUnit(String rentalUnitId) {
        return occupancyHistoryRepository.findByRentalUnitIdAndMoveOutDateIsNull(rentalUnitId)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("This unit is currently vacant"));
    }

    public OccupancyResponse getCurrentOccupancyForProperty(String propertyId) {
        return occupancyHistoryRepository
                .findByPropertyIdAndOccupancyTypeAndMoveOutDateIsNull(propertyId, OccupancyType.WHOLE_PROPERTY)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("This property is not currently under a whole-property occupancy"));
    }

    private Lease findLeaseOrThrow(String leaseId) {
        return leaseRepository.findById(leaseId)
                .orElseThrow(() -> new ResourceNotFoundException("Lease not found"));
    }

    private OccupancyResponse toResponse(OccupancyHistory history) {
        OccupancyResponse response = new OccupancyResponse();
        response.setId(history.getId());
        response.setOccupancyType(history.getOccupancyType());
        response.setRentalUnitId(history.getRentalUnitId());
        response.setPropertyId(history.getPropertyId());
        response.setTenantId(history.getTenantId());
        response.setLeaseId(history.getLeaseId());
        response.setMoveInDate(history.getMoveInDate());
        response.setMoveOutDate(history.getMoveOutDate());

        if (history.getPropertyId() != null) {
            propertyRepository.findById(history.getPropertyId())
                    .ifPresent(property -> response.setProperty(toPropertySummary(property)));
        }

        if (history.getRentalUnitId() != null) {
            rentalUnitRepository.findById(history.getRentalUnitId())
                    .ifPresent(unit -> response.setRentalUnit(toRentalUnitSummary(unit)));
        }

        if (history.getTenantId() != null) {
            tenantRepository.findById(history.getTenantId())
                    .ifPresent(tenant -> response.setTenant(toTenantSummary(tenant)));
        }

        return response;
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

    private TenantSummaryResponse toTenantSummary(Tenant tenant) {
        TenantSummaryResponse summary = new TenantSummaryResponse();
        summary.setId(tenant.getId());
        summary.setFullName(tenant.getFullName());
        summary.setPhoneNumber(tenant.getPhoneNumber());
        summary.setEmail(tenant.getEmail());
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