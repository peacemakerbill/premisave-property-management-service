package com.premisave.property.service;

import com.premisave.property.dto.request.RentalUnitRequest;
import com.premisave.property.dto.response.RentalUnitResponse;
import com.premisave.property.entity.RentalUnit;
import com.premisave.property.enums.UnitStatus;
import com.premisave.property.exception.ResourceNotFoundException;
import com.premisave.property.repository.RentalUnitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RentalUnitService {

    private final RentalUnitRepository rentalUnitRepository;

    @Transactional
    public RentalUnitResponse createUnit(String propertyId, RentalUnitRequest request) {
        RentalUnit unit = new RentalUnit();
        unit.setPropertyId(propertyId);
        unit.setUnitNumber(request.getUnitNumber());
        unit.setFloor(request.getFloor());
        unit.setRentAmount(request.getRentAmount());
        unit.setSecurityDeposit(request.getSecurityDeposit());
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

        return toResponse(rentalUnitRepository.save(unit));
    }

    public List<RentalUnitResponse> getUnitsByProperty(String propertyId) {
        return rentalUnitRepository.findByPropertyId(propertyId).stream()
                .map(this::toResponse)
                .toList();
    }

    public RentalUnitResponse getUnitById(String unitId) {
        return toResponse(findUnitOrThrow(unitId));
    }

    private RentalUnit findUnitOrThrow(String unitId) {
        return rentalUnitRepository.findById(unitId)
                .orElseThrow(() -> new ResourceNotFoundException("Rental unit not found"));
    }

    private RentalUnitResponse toResponse(RentalUnit unit) {
        RentalUnitResponse response = new RentalUnitResponse();
        response.setId(unit.getId());
        response.setUnitNumber(unit.getUnitNumber());
        response.setRentAmount(unit.getRentAmount());
        response.setStatus(unit.getStatus());
        return response;
    }
}