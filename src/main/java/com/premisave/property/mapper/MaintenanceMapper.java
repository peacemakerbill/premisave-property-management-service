package com.premisave.property.mapper;

import com.premisave.property.dto.request.MaintenanceRequest;
import com.premisave.property.dto.response.MaintenanceResponse;
import com.premisave.property.entity.Maintenance;   // Updated entity name
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface MaintenanceMapper {

    Maintenance toEntity(MaintenanceRequest dto);

    MaintenanceResponse toResponse(Maintenance entity);

    void updateEntity(@MappingTarget Maintenance entity, MaintenanceRequest dto);
}