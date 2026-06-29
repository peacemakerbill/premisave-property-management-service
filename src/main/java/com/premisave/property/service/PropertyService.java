package com.premisave.property.service;

import com.premisave.property.dto.request.CreatePropertyRequest;
import com.premisave.property.dto.response.PropertyResponse;
import com.premisave.property.entity.Property;
import com.premisave.property.mapper.PropertyMapper;
import com.premisave.property.repository.PropertyRepository;
import com.premisave.property.validation.PropertyValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PropertyService {

    private final PropertyRepository propertyRepository;
    private final PropertyMapper propertyMapper;
    private final PropertyValidator propertyValidator;

    @Transactional
    public PropertyResponse createProperty(CreatePropertyRequest request, String ownerId) {
        propertyValidator.validateCreateProperty(request);

        Property property = propertyMapper.toEntity(request);
        property.setOwnerId(ownerId);
        property.setIsActive(true);

        Property saved = propertyRepository.save(property);
        return propertyMapper.toResponse(saved);
    }

    public List<PropertyResponse> getOwnerProperties(String ownerId) {
        return propertyRepository.findByOwnerId(ownerId)
                .stream()
                .map(propertyMapper::toResponse)
                .toList();
    }

    public PropertyResponse getPropertyById(String id) {
        Property property = propertyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Property not found"));
        return propertyMapper.toResponse(property);
    }
}