package com.premisave.property.health;

import com.premisave.property.exception.ResourceNotFoundException;
import com.premisave.property.exception.ServiceOfflineException;
import com.premisave.property.exception.UnauthorizedException;
import feign.FeignException;

import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.net.http.HttpConnectTimeoutException;

/** Classifies failures of calls to other services, so the frontend gets a proper message. */
public final class FeignFailures {

    private FeignFailures() {
    }

    /**
     * True only when the request never reached the other service: connection refused, host
     * unreachable, DNS failure or a connect timeout. A READ timeout is NOT this — the request
     * may have been processed. Safe to conclude "nothing happened" (e.g. no money moved).
     */
    public static boolean neverReached(Throwable error) {
        int depth = 0;
        for (Throwable t = error; t != null && depth < 10; t = t.getCause(), depth++) {
            if (t instanceof ConnectException || t instanceof UnknownHostException
                    || t instanceof NoRouteToHostException || t instanceof HttpConnectTimeoutException) {
                return true;
            }
            if (t instanceof SocketTimeoutException && t.getMessage() != null
                    && t.getMessage().toLowerCase().contains("connect")) {
                return true;
            }
        }
        return false;
    }

    /**
     * "The service isn't there": never reached, any network-level failure (status -1, including
     * read timeouts), or a gateway/service answering 502/503/504. Use only for calls where the
     * outcome doesn't matter (reads) — for money movement use {@link #neverReached}.
     */
    public static boolean looksOffline(FeignException e) {
        int status = e.status();
        return neverReached(e) || status == -1 || status == 502 || status == 503 || status == 504;
    }

    /**
     * For calls that forward the user's own JWT (e.g. auth-service /profile/me): offline => friendly
     * 503; 401/403 means the user's session; 404 means no such account; any other failure is
     * treated as the service not being available.
     */
    public static RuntimeException toServiceException(FeignException e, ExternalService service,
                                                      String action, String reassurance) {
        if (looksOffline(e)) {
            return new ServiceOfflineException(service, action, reassurance);
        }
        int status = e.status();
        if (status == 401 || status == 403) {
            return new UnauthorizedException("We couldn't verify your session. Please sign in again.");
        }
        if (status == 404) {
            return new ResourceNotFoundException("We couldn't find your account. Please sign in again.");
        }
        return new ServiceOfflineException(service, action, reassurance);
    }
}