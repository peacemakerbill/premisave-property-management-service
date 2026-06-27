package com.premisave.property.mapper;

import com.premisave.property.dto.response.RentalUnitResponse;
import com.premisave.property.entity.RentalUnit;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface RentalUnitMapper {

    RentalUnitResponse toResponse(RentalUnit unit);
}