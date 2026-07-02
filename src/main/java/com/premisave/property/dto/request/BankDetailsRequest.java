package com.premisave.property.dto.request;

import lombok.Data;

@Data
public class BankDetailsRequest {
    private String bankName;
    private String accountNumber;
    private String accountName;
    private String branchCode;
}