package com.premisave.property.service;

import com.premisave.property.dto.request.CreateLeaseRequest;
import com.premisave.property.dto.request.UpdateLeaseRequest;
import com.premisave.property.dto.response.LeaseResponse;
import com.premisave.property.entity.Lease;
import com.premisave.property.entity.Property;
import com.premisave.property.entity.RentalUnit;
import com.premisave.property.enums.LeaseStatus;
import com.premisave.property.enums.LeaseType;
import com.premisave.property.enums.UnitStatus;
import com.premisave.property.exception.BadRequestException;
import com.premisave.property.exception.ConflictException;
import com.premisave.property.exception.ResourceNotFoundException;
import com.premisave.property.repository.LeaseRepository;
import com.premisave.property.repository.PropertyRepository;
import com.premisave.property.repository.RentalUnitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LeaseService {

    private final LeaseRepository leaseRepository;
    private final RentalUnitRepository rentalUnitRepository;
    private final PropertyRepository propertyRepository;
    private final RentScheduleService rentScheduleService;

    @Transactional
    public LeaseResponse createLease(CreateLeaseRequest request) {
        if (!request.getEndDate().isAfter(request.getStartDate())) {
            throw new BadRequestException("endDate must be after startDate");
        }

        boolean hasUnit = request.getRentalUnitId() != null && !request.getRentalUnitId().isBlank();
        boolean hasProperty = request.getPropertyId() != null && !request.getPropertyId().isBlank();

        if (hasUnit == hasProperty) {
            throw new BadRequestException("Provide exactly one of rentalUnitId or propertyId");
        }

        Lease saved = hasUnit
                ? createUnitLease(request)
                : createWholePropertyLease(request);

        rentScheduleService.generateMonthlySchedule(saved.getId());

        return toResponse(saved);
    }

    private Lease createUnitLease(CreateLeaseRequest request) {
        RentalUnit unit = rentalUnitRepository.findById(request.getRentalUnitId())
                .orElseThrow(() -> new ResourceNotFoundException("Rental unit not found"));

        if (unit.getStatus() != UnitStatus.VACANT) {
            throw new ConflictException("Rental unit is not available for lease");
        }

        Lease lease = new Lease();
        lease.setTenantId(request.getTenantId());
        lease.setRentalUnitId(unit.getId());
        lease.setPropertyId(unit.getPropertyId());
        lease.setLeaseType(LeaseType.UNIT);
        lease.setStartDate(request.getStartDate());
        lease.setEndDate(request.getEndDate());
        lease.setMonthlyRent(request.getMonthlyRent());
        lease.setSecurityDeposit(unit.getSecurityDeposit());
        lease.setStatus(LeaseStatus.ACTIVE);

        Lease saved = leaseRepository.save(lease);

        unit.setStatus(UnitStatus.OCCUPIED);
        rentalUnitRepository.save(unit);

        return saved;
    }

    private Lease createWholePropertyLease(CreateLeaseRequest request) {
        Property property = propertyRepository.findById(request.getPropertyId())
                .orElseThrow(() -> new ResourceNotFoundException("Property not found"));

        if (!Boolean.TRUE.equals(property.getIsActive())) {
            throw new ConflictException("Property is not active/available for lease");
        }

        if (leaseRepository.existsByPropertyIdAndLeaseTypeAndStatus(
                property.getId(), LeaseType.WHOLE_PROPERTY, LeaseStatus.ACTIVE)) {
            throw new ConflictException("This property already has an active whole-property lease");
        }

        Lease lease = new Lease();
        lease.setTenantId(request.getTenantId());
        lease.setRentalUnitId(null);
        lease.setPropertyId(property.getId());
        lease.setLeaseType(LeaseType.WHOLE_PROPERTY);
        lease.setStartDate(request.getStartDate());
        lease.setEndDate(request.getEndDate());
        lease.setMonthlyRent(request.getMonthlyRent());
        lease.setSecurityDeposit(request.getSecurityDeposit());
        lease.setStatus(LeaseStatus.ACTIVE);

        return leaseRepository.save(lease);
    }

    public LeaseResponse getLease(String id) {
        return toResponse(findLeaseOrThrow(id));
    }

    public List<LeaseResponse> getLeasesByTenant(String tenantId) {
        return leaseRepository.findByTenantId(tenantId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public LeaseResponse updateLease(String id, UpdateLeaseRequest request) {
        Lease lease = findLeaseOrThrow(id);

        if (request.getEndDate() != null) {
            lease.setEndDate(request.getEndDate());
        }

        if (request.getStatus() != null) {
            lease.setStatus(request.getStatus());
            if (request.getStatus() == LeaseStatus.TERMINATED || request.getStatus() == LeaseStatus.EXPIRED) {
                releaseUnitIfApplicable(lease);
            }
        }

        return toResponse(leaseRepository.save(lease));
    }

    @Transactional
    public LeaseResponse terminateLease(String id) {
        Lease lease = findLeaseOrThrow(id);

        if (lease.getStatus() == LeaseStatus.TERMINATED) {
            throw new ConflictException("This lease has already been terminated");
        }

        lease.setStatus(LeaseStatus.TERMINATED);
        Lease saved = leaseRepository.save(lease);
        releaseUnitIfApplicable(lease);
        return toResponse(saved);
    }

    private void releaseUnitIfApplicable(Lease lease) {
        if (lease.getRentalUnitId() == null) {
            return; // whole-property lease — no unit to release
        }
        rentalUnitRepository.findById(lease.getRentalUnitId()).ifPresent(unit -> {
            unit.setStatus(UnitStatus.VACANT);
            rentalUnitRepository.save(unit);
        });
    }

    private Lease findLeaseOrThrow(String id) {
        return leaseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lease not found"));
    }

    private LeaseResponse toResponse(Lease lease) {
        LeaseResponse response = new LeaseResponse();
        response.setId(lease.getId());
        response.setTenantId(lease.getTenantId());
        response.setRentalUnitId(lease.getRentalUnitId());
        response.setPropertyId(lease.getPropertyId());
        response.setLeaseType(lease.getLeaseType());
        response.setStartDate(lease.getStartDate());
        response.setEndDate(lease.getEndDate());
        response.setMonthlyRent(lease.getMonthlyRent());
        response.setSecurityDeposit(lease.getSecurityDeposit());
        response.setStatus(lease.getStatus());
        return response;
    }
}