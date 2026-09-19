package com.premisave.property.health;

import com.premisave.property.exception.ServiceOfflineException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Knows whether auth-service and wallet-service are reachable, so endpoints that depend on
 * them can fail fast with a clear message instead of timing out or failing halfway.
 *
 * How it decides:
 *  - It sends GET {service-url}{health-path} with short timeouts. ANY HTTP answer means the
 *    service is up (even 401/404), except 502/503/504, which mean "no backend". A refused
 *    connection, unknown host or timeout means it is down. So it works whether or not the
 *    health path is protected or even exists.
 *  - Results are cached: an UP result for 30s, a DOWN result for only 3s (so recovery is
 *    noticed quickly). A background job refreshes both every 10s, so requests almost never
 *    wait for a probe. A real call that fails to connect also flips the service to DOWN
 *    immediately (markDown).
 *  - If the check itself breaks (bad URL, interrupted), it fails OPEN: a broken checker must
 *    never block payments. Set service-health.enabled=false to switch the checks off.
 */
@Slf4j
@Component
public class ServiceHealthMonitor {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(2);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(3);
    private static final long UP_TTL_MILLIS = 30_000;
    private static final long DOWN_TTL_MILLIS = 3_000;
    private static final long PROBE_DEDUP_MILLIS = 1_000;

    private record State(boolean online, long checkedAtMillis) {
    }

    private final Map<ExternalService, State> states = new ConcurrentHashMap<>();
    private final Map<ExternalService, Object> probeLocks = new EnumMap<>(ExternalService.class);
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(CONNECT_TIMEOUT)
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    @Value("${service-health.enabled:true}")
    private boolean enabled;

    @Value("${auth-service.url:http://localhost:8080}")
    private String authUrl;

    @Value("${wallet-service.url:http://localhost:8084}")
    private String walletUrl;

    @Value("${service-health.auth.path:/health}")
    private String authHealthPath;

    @Value("${service-health.wallet.path:/system/health}")
    private String walletHealthPath;

    public ServiceHealthMonitor() {
        for (ExternalService service : ExternalService.values()) {
            probeLocks.put(service, new Object());
        }
    }

    /** Throws ServiceOfflineException (HTTP 503, friendly message) if any listed service is down. */
    public void requireOnline(String action, String reassurance, ExternalService... services) {
        if (!enabled) {
            return;
        }
        for (ExternalService service : services) {
            if (!isOnline(service)) {
                throw new ServiceOfflineException(service, action, reassurance);
            }
        }
    }

    public boolean isOnline(ExternalService service) {
        if (!enabled) {
            return true;
        }
        State state = states.get(service);
        if (state != null && age(state) < (state.online() ? UP_TTL_MILLIS : DOWN_TTL_MILLIS)) {
            return state.online();
        }
        return probe(service);
    }

    /** A real call just failed to reach the service — stop sending traffic at it right away. */
    public void markDown(ExternalService service) {
        if (service != null) {
            record(service, false);
        }
    }

    /** Which dependency a request URL belongs to, or null — used to name the service in fallback errors. */
    public ExternalService serviceForUrl(String url) {
        if (url == null) {
            return null;
        }
        if (authUrl != null && url.startsWith(stripTrailingSlash(authUrl))) {
            return ExternalService.AUTH;
        }
        if (walletUrl != null && url.startsWith(stripTrailingSlash(walletUrl))) {
            return ExternalService.WALLET;
        }
        return null;
    }

    @Scheduled(fixedDelay = 10_000, initialDelay = 5_000)
    public void refresh() {
        if (!enabled) {
            return;
        }
        for (ExternalService service : ExternalService.values()) {
            probe(service);
        }
    }

    // ------------------------------------------------------------------

    private boolean probe(ExternalService service) {
        synchronized (probeLocks.get(service)) {
            State state = states.get(service);
            if (state != null && age(state) < PROBE_DEDUP_MILLIS) {
                return state.online();   // another thread probed a moment ago
            }
            boolean online = sendProbe(service);
            record(service, online);
            return online;
        }
    }

    private boolean sendProbe(ExternalService service) {
        String base = service == ExternalService.AUTH ? authUrl : walletUrl;
        String path = service == ExternalService.AUTH ? authHealthPath : walletHealthPath;
        try {
            URI uri = URI.create(stripTrailingSlash(base) + path);
            HttpRequest request = HttpRequest.newBuilder(uri).timeout(REQUEST_TIMEOUT).GET().build();
            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            int status = response.statusCode();
            return status != 502 && status != 503 && status != 504;
        } catch (IOException e) {
            return false;   // connection refused / unknown host / timeout
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return true;    // shutting down — don't claim a service is down because of that
        } catch (RuntimeException e) {
            log.warn("Health check for {} is misconfigured ({}); treating it as online", service, e.getMessage());
            return true;    // fail open: a broken checker must not block requests
        }
    }

    private void record(ExternalService service, boolean online) {
        State previous = states.put(service, new State(online, System.currentTimeMillis()));
        if (previous == null ? !online : previous.online() != online) {
            if (online) {
                log.info("{} is back online", service.getDisplayName());
            } else {
                log.warn("{} is OFFLINE — dependent endpoints will answer 503 until it is reachable again",
                        service.getDisplayName());
            }
        }
    }

    private static long age(State state) {
        return System.currentTimeMillis() - state.checkedAtMillis();
    }

    private static String stripTrailingSlash(String url) {
        return url != null && url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}