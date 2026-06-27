package com.premisave.property.dto.response;

import lombok.Data;

@Data
public class AddressResponse {

    private String street;
    private String city;
    private String state;
    private String country;
    private String postalCode;
    private String landmark;
}