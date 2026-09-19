package com.premisave.property.health;

/** The other Premisave services this service depends on, with the names users and the frontend see. */
public enum ExternalService {

    AUTH("auth-service", "Authentication service", "authentication service", "Authentication service is offline"),
    WALLET("wallet-service", "Premisave Wallet", "wallet", "Wallet is offline");

    private final String targetName;    // stable id used in JSON, e.g. "wallet-service"
    private final String displayName;   // "Premisave Wallet"
    private final String label;         // fits "our ___ is offline": "our wallet is offline"
    private final String offlineTitle;  // short headline for an error dialog

    ExternalService(String targetName, String displayName, String label, String offlineTitle) {
        this.targetName = targetName;
        this.displayName = displayName;
        this.label = label;
        this.offlineTitle = offlineTitle;
    }

    public String getTargetName() { return targetName; }
    public String getDisplayName() { return displayName; }
    public String getLabel() { return label; }
    public String getOfflineTitle() { return offlineTitle; }
}