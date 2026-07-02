package com.premisave.property.dto.response;

import lombok.Data;

@Data
public class BankDetailsResponse {
    private String bankName;
    private String accountName;
    private String accountNumberMasked;
}