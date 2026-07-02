package com.premisave.property.service;

import com.premisave.property.dto.request.CreateLeaseRequest;
import com.premisave.property.dto.request.UpdateLeaseRequest;
import com.premisave.property.dto.response.LeaseResponse;
import com.premisave.property.entity.Lease;
import com.premisave.property.entity.RentalUnit;
import com.premisave.property.enums.LeaseStatus;
import com.premisave.property.enums.UnitStatus;
import com.premisave.property.exception.BadRequestException;
import com.premisave.property.exception.ConflictException;
import com.premisave.property.exception.ResourceNotFoundException;
import com.premisave.property.repository.LeaseRepository;
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
    private final RentScheduleService rentScheduleService;

    @Transactional
    public LeaseResponse createLease(CreateLeaseRequest request) {
        if (!request.getEndDate().isAfter(request.getStartDate())) {
            throw new BadRequestException("endDate must be after startDate");
        }

        RentalUnit unit = rentalUnitRepository.findById(request.getRentalUnitId())
                .orElseThrow(() -> new ResourceNotFoundException("Rental unit not found"));

        if (unit.getStatus() != UnitStatus.VACANT) {
            throw new ConflictException("Rental unit is not available for lease");
        }

        Lease lease = new Lease();
        lease.setTenantId(request.getTenantId());
        lease.setRentalUnitId(unit.getId());
        lease.setPropertyId(unit.getPropertyId());
        lease.setStartDate(request.getStartDate());
        lease.setEndDate(request.getEndDate());
        lease.setMonthlyRent(request.getMonthlyRent());
        lease.setSecurityDeposit(unit.getSecurityDeposit());
        lease.setStatus(LeaseStatus.ACTIVE);

        Lease saved = leaseRepository.save(lease);

        unit.setStatus(UnitStatus.OCCUPIED);
        rentalUnitRepository.save(unit);

        rentScheduleService.generateMonthlySchedule(saved.getId());

        return toResponse(saved);
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
                releaseUnit(lease.getRentalUnitId());
            }
        }

        return toResponse(leaseRepository.save(lease));
    }

    @Transactional
    public LeaseResponse terminateLease(String id) {
        Lease lease = findLeaseOrThrow(id);
        lease.setStatus(LeaseStatus.TERMINATED);
        Lease saved = leaseRepository.save(lease);
        releaseUnit(lease.getRentalUnitId());
        return toResponse(saved);
    }

    private void releaseUnit(String rentalUnitId) {
        rentalUnitRepository.findById(rentalUnitId).ifPresent(unit -> {
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
        response.setStartDate(lease.getStartDate());
        response.setEndDate(lease.getEndDate());
        response.setMonthlyRent(lease.getMonthlyRent());
        response.setStatus(lease.getStatus());
        return response;
    }
}