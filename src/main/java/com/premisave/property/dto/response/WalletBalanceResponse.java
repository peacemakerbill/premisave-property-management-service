package com.premisave.property.dto.response;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class WalletBalanceResponse {

    private BigDecimal balance;
    private String currency;
    private boolean frozen;
}