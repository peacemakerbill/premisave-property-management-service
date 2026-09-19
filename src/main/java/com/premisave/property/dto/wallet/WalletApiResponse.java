package com.premisave.property.dto.wallet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/** wallet-service's standard response envelope. */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class WalletApiResponse<T> {
    private boolean success;
    private String message;
    private T data;
    private String timestamp;   // kept as String — wallet-service sends microsecond precision
}