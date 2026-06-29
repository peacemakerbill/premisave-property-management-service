package com.premisave.property.service;

import com.premisave.property.dto.response.RentalUnitResponse;
import com.premisave.property.entity.RentalUnit;
import com.premisave.property.mapper.RentalUnitMapper;
import com.premisave.property.repository.RentalUnitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RentalUnitService {

    private final RentalUnitRepository rentalUnitRepository;
    private final RentalUnitMapper rentalUnitMapper;

    public List<RentalUnitResponse> getUnitsByProperty(String propertyId) {
        return rentalUnitRepository.findByPropertyId(propertyId)
                .stream()
                .map(rentalUnitMapper::toResponse)
                .toList();
    }

    public RentalUnitResponse getUnitById(String unitId) {
        RentalUnit unit = rentalUnitRepository.findById(unitId)
                .orElseThrow(() -> new RuntimeException("Rental unit not found"));
        return rentalUnitMapper.toResponse(unit);
    }
}