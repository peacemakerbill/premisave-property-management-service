package com.premisave.property.mapper;

import com.premisave.property.dto.request.WorkOrderRequest;
import com.premisave.property.entity.WorkOrder;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface WorkOrderMapper {

    WorkOrder toEntity(WorkOrderRequest request);
}