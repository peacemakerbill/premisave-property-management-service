package com.premisave.property.dto.response;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class RefundCheckResponse {
    private BigDecimal depositAmount;
    private BigDecimal alreadyRefunded;
    private BigDecimal remainingRefundable;
    private BigDecimal requestedAmount;

    // true if requestedAmount == remainingRefundable (i.e. this refund would
    // fully close the deposit out)
    private boolean fullRefund;

    // true if requestedAmount < remainingRefundable
    private boolean partialRefund;

    // true if requestedAmount > remainingRefundable — frontend should block
    // submission and show this message rather than call the real endpoint
    private boolean exceedsRemainingBalance;

    // Convenience for the frontend — same rule the real refund endpoint
    // enforces: reason is mandatory whenever this isn't a full refund.
    private boolean reasonRequired;

    private String message;
}