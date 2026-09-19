package com.premisave.property.exception;

import com.premisave.property.health.ExternalService;

/**
 * A service this endpoint depends on is unreachable. Mapped to HTTP 503 with a friendly
 * message and a machine-readable code by GlobalExceptionHandler.
 */
public class ServiceOfflineException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public static final int RETRY_AFTER_SECONDS = 30;

    private final transient ExternalService service;   // null if it could not be determined

    /**
     * @param action      what the user was trying to do, phrased to follow "We can't ..." (null => generic)
     * @param reassurance optional extra sentence, e.g. "Nothing has been charged." (null/blank => none)
     */
    public ServiceOfflineException(ExternalService service, String action, String reassurance) {
        super(buildMessage(service, action, reassurance));
        this.service = service;
    }

    public ExternalService getService() {
        return service;
    }

    /** Short headline for an error dialog, e.g. "Wallet is offline". */
    public String getTitle() {
        return service != null ? service.getOfflineTitle() : "Service is offline";
    }

    private static String buildMessage(ExternalService service, String action, String reassurance) {
        String what = action != null && !action.isBlank() ? action : "complete this request";
        String who = service != null ? "our " + service.getLabel() : "one of our services";
        StringBuilder message = new StringBuilder("We can't ").append(what)
                .append(" right now because ").append(who).append(" is offline.");
        if (reassurance != null && !reassurance.isBlank()) {
            message.append(' ').append(reassurance.trim());
        }
        return message.append(" Please try again in a few minutes.").toString();
    }
}