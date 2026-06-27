package com.premisave.property.mapper;

import com.premisave.property.dto.request.TenantRegistrationRequest;
import com.premisave.property.dto.request.UpdateTenantRequest;
import com.premisave.property.dto.response.TenantResponse;
import com.premisave.property.entity.Tenant;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface TenantMapper {

    Tenant toEntity(TenantRegistrationRequest request);

    TenantResponse toResponse(Tenant tenant);

    void updateEntity(@MappingTarget Tenant tenant, UpdateTenantRequest request);
}