package com.premisave.property.mapper;

import com.premisave.property.dto.request.RentPaymentRequest;
import com.premisave.property.dto.response.RentPaymentResponse;
import com.premisave.property.entity.RentPayment;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface RentPaymentMapper {

    RentPayment toEntity(RentPaymentRequest request);

    RentPaymentResponse toResponse(RentPayment payment);
}