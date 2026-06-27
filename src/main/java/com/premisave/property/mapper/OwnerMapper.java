package com.premisave.property.mapper;

import com.premisave.property.dto.request.CreateOwnerRequest;
import com.premisave.property.dto.request.UpdateOwnerRequest;
import com.premisave.property.dto.response.OwnerResponse;
import com.premisave.property.entity.Owner;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface OwnerMapper {

    Owner toEntity(CreateOwnerRequest request);

    OwnerResponse toResponse(Owner owner);

    void updateEntity(@MappingTarget Owner owner, UpdateOwnerRequest request);
}