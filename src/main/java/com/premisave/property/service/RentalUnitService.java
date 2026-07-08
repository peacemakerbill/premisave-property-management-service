package com.premisave.property.service;

import com.premisave.property.dto.request.RentalUnitRequest;
import com.premisave.property.dto.response.RentalUnitResponse;
import com.premisave.property.entity.RentalUnit;
import com.premisave.property.enums.LeaseStatus;
import com.premisave.property.enums.UnitStatus;
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
public class RentalUnitService {

    private final RentalUnitRepository rentalUnitRepository;
    private final LeaseRepository leaseRepository;

    @Transactional
    public RentalUnitResponse createUnit(String propertyId, RentalUnitRequest request) {
        RentalUnit unit = new RentalUnit();
        unit.setPropertyId(propertyId);
        unit.setUnitNumber(request.getUnitNumber());
        unit.setFloor(request.getFloor());
        unit.setRentAmount(request.getRentAmount());
        unit.setSecurityDeposit(request.getSecurityDeposit());
        if (request.getDepositRequired() != null) {
            unit.setDepositRequired(request.getDepositRequired());
        }
        unit.setStatus(UnitStatus.VACANT);

        return toResponse(rentalUnitRepository.save(unit));
    }

    @Transactional
    public RentalUnitResponse updateUnit(String unitId, RentalUnitRequest request) {
        RentalUnit unit = findUnitOrThrow(unitId);

        if (request.getUnitNumber() != null) {
            unit.setUnitNumber(request.getUnitNumber());
        }
        if (request.getFloor() != null) {
            unit.setFloor(request.getFloor());
        }
        if (request.getRentAmount() != null) {
            unit.setRentAmount(request.getRentAmount());
        }
        if (request.getSecurityDeposit() != null) {
            unit.setSecurityDeposit(request.getSecurityDeposit());
        }
        if (request.getDepositRequired() != null) {
            unit.setDepositRequired(request.getDepositRequired());
        }
        if (request.getStatus() != null) {
            applyStatusChange(unit, request.getStatus());
        }

        return toResponse(rentalUnitRepository.save(unit));
    }

    @Transactional
    public void deleteUnit(String unitId) {
        RentalUnit unit = findUnitOrThrow(unitId);

        if (leaseRepository.existsByRentalUnitIdAndStatus(unitId, LeaseStatus.ACTIVE)) {
            throw new ConflictException(
                    "This unit has an active lease and cannot be deleted. Terminate the lease first.");
        }

        rentalUnitRepository.delete(unit);
    }

    public List<RentalUnitResponse> getUnitsByProperty(String propertyId) {
        return rentalUnitRepository.findByPropertyId(propertyId).stream()
                .map(this::toResponse)
                .toList();
    }

    public RentalUnitResponse getUnitById(String unitId) {
        return toResponse(findUnitOrThrow(unitId));
    }

    private void applyStatusChange(RentalUnit unit, UnitStatus newStatus) {
        boolean hasActiveLease = leaseRepository.existsByRentalUnitIdAndStatus(unit.getId(), LeaseStatus.ACTIVE);

        if (newStatus == UnitStatus.VACANT && hasActiveLease) {
            throw new ConflictException(
                    "Cannot mark this unit VACANT while it has an active lease. Terminate the lease first.");
        }

        unit.setStatus(newStatus);
    }

    private RentalUnit findUnitOrThrow(String unitId) {
        return rentalUnitRepository.findById(unitId)
                .orElseThrow(() -> new ResourceNotFoundException("Rental unit not found"));
    }

    private RentalUnitResponse toResponse(RentalUnit unit) {
        RentalUnitResponse response = new RentalUnitResponse();
        response.setId(unit.getId());
        response.setPropertyId(unit.getPropertyId());
        response.setUnitNumber(unit.getUnitNumber());
        response.setFloor(unit.getFloor());
        response.setRentAmount(unit.getRentAmount());
        response.setSecurityDeposit(unit.getSecurityDeposit());
        response.setDepositRequired(unit.getDepositRequired());
        response.setStatus(unit.getStatus());
        return response;
    }
}