package com.premisave.property.dto.wallet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * Payload inside the envelope's {@code data}. For transfers, transactionId is
 * the transfer's reference (not a database id) — see wallet-service docs.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class WalletTransactionResult {
    private boolean success;
    private String transactionId;
    private String message;
}