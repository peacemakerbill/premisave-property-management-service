package com.premisave.property.dto.wallet;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/** Body for wallet-service POST /internal/transfer (InternalTransferRequest). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletTransferRequest {
    private String senderUserId;
    private String recipientAccountNumber;
    private BigDecimal amount;
    private String description;
    private String reference;
    private String initiatedBy;
}