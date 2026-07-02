package com.premisave.property.dto.request;

import lombok.Data;

@Data
public class UpdateOwnerRequest {

    private String fullName;
    private String phoneNumber;
    private String email;

    private AddressRequest address;
    private BankDetailsRequest bankDetails;
}