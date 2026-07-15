package com.premisave.property.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class RentBalanceResponse {
    private String tenantId;
    private String leaseId;        // set for lease-based balances
    private String rentalUnitId;   // set for direct-unit balances

    // Positive = arrears owed. Negative = credit/overpayment on file.
    private BigDecimal balance;

    // Convenience fields so the frontend never has to inspect the sign
    // of `balance` itself.
    private BigDecimal arrearsOwed;      // 0 if balance <= 0
    private BigDecimal creditAvailable;  // 0 if balance >= 0
    private String statusMessage;

    private LocalDateTime lastChargeAt;
    private LocalDateTime lastPaymentAt;
}