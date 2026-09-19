package com.premisave.property.util;

public class Constants {

    // Premisave wallets are USD-denominated, and property-service amounts are
    // sent to wallet-service as-is, so every amount here is USD.
    public static final String DEFAULT_CURRENCY = "USD";
    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;

    // Audit constants
    public static final String SYSTEM_USER = "SYSTEM";

    private Constants() {
        // Utility class
    }
}