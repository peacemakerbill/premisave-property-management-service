package com.premisave.property.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Currency;

@Data
public class Money {

    private BigDecimal amount;
    private Currency currency = Currency.getInstance("KES"); // Default to Kenyan Shilling
}