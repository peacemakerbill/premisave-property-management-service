package com.premisave.property.mapper;

import com.premisave.property.dto.request.CreateLeaseRequest;
import com.premisave.property.dto.request.UpdateLeaseRequest;
import com.premisave.property.dto.response.LeaseResponse;
import com.premisave.property.entity.Lease;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface LeaseMapper {

    Lease toEntity(CreateLeaseRequest request);

    LeaseResponse toResponse(Lease lease);

    void updateEntity(@MappingTarget Lease lease, UpdateLeaseRequest request);
}