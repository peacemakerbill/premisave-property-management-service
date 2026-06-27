package com.premisave.property.mapper;

import com.premisave.property.dto.request.CreatePropertyRequest;
import com.premisave.property.dto.response.PropertyResponse;
import com.premisave.property.entity.Property;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface PropertyMapper {

    Property toEntity(CreatePropertyRequest request);

    PropertyResponse toResponse(Property property);

    void updateEntity(@MappingTarget Property property, CreatePropertyRequest request);
}