package com.premisave.property.enums;

public enum WalletTransferStatus {
    INITIATED,   // record created; wallet call not confirmed yet (or outcome unknown after a timeout)
    COMPLETED,   // wallet confirmed the transfer; local booking not finished yet
    BOOKED,      // wallet confirmed AND the payment/bill has been booked locally
    FAILED       // wallet rejected the transfer; no money moved
}