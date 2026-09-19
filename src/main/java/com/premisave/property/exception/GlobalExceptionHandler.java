package com.premisave.property.exception;

import com.premisave.property.health.ExternalService;
import com.premisave.property.health.FeignFailures;
import com.premisave.property.health.ServiceHealthMonitor;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final ServiceHealthMonitor healthMonitor;

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, Object> response = new HashMap<>();
        Map<String, String> errors = new HashMap<>();

        ex.getBindingResult().getFieldErrors().forEach(error ->
            errors.put(error.getField(), error.getDefaultMessage())
        );

        response.put("timestamp", LocalDateTime.now());
        response.put("status", HttpStatus.BAD_REQUEST.value());
        response.put("error", "Validation Error");
        response.put("errors", errors);

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleResourceNotFound(ResourceNotFoundException ex) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<Map<String, Object>> handleConflict(ConflictException ex) {
        return buildErrorResponse(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<Map<String, Object>> handleUnauthorized(UnauthorizedException ex) {
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAuthorizationDenied(AuthorizationDeniedException ex) {
        return buildErrorResponse(HttpStatus.FORBIDDEN,
                "You do not have permission to perform this action");
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(BadRequestException ex) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(ValidationException ex) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    // auth-service or wallet-service is offline. code=SERVICE_OFFLINE plus the service involved, so the
    // frontend can show a proper "service offline" state instead of a generic error.
    @ExceptionHandler(ServiceOfflineException.class)
    public ResponseEntity<Map<String, Object>> handleServiceOffline(ServiceOfflineException ex) {
        log.warn("Dependency offline: {}", ex.getService());
        return offlineResponse(ex);
    }

    // The payment could not be confirmed / recorded (not the same as the wallet being offline).
    // Retryable with the same reference; the code tells the frontend which case it is.
    @ExceptionHandler(WalletServiceException.class)
    public ResponseEntity<Map<String, Object>> handleWalletService(WalletServiceException ex) {
        log.warn("Wallet payment problem [{}]: {}", ex.getCode(), ex.getMessage());
        Map<String, Object> body = errorBody(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage());
        if (ex.getCode() != null) {
            body.put("code", ex.getCode());
        }
        body.put("retryable", true);
        return new ResponseEntity<>(body, HttpStatus.SERVICE_UNAVAILABLE);
    }

    // Safety net for any call to another service that no code path handled — instead of a bare 500.
    @ExceptionHandler(FeignException.class)
    public ResponseEntity<Map<String, Object>> handleFeign(FeignException ex) {
        log.error("Call to another service failed (HTTP {}): {}", ex.status(), ex.getMessage());

        if (FeignFailures.looksOffline(ex) || ex.status() >= 500) {
            ExternalService service = ex.request() != null ? healthMonitor.serviceForUrl(ex.request().url()) : null;
            healthMonitor.markDown(service);
            return offlineResponse(new ServiceOfflineException(service, null, null));
        }
        return buildErrorResponse(HttpStatus.BAD_GATEWAY,
                "A request to another Premisave service was rejected. Please try again, "
                        + "and contact support if it keeps happening.");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneralException(Exception ex) {
        log.error("Unhandled exception", ex);
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
    }

    private ResponseEntity<Map<String, Object>> offlineResponse(ServiceOfflineException ex) {
        Map<String, Object> body = errorBody(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage());
        body.put("code", "SERVICE_OFFLINE");
        body.put("title", ex.getTitle());
        body.put("service", ex.getService() != null ? ex.getService().getTargetName() : null);
        body.put("retryable", true);
        body.put("retryAfterSeconds", ServiceOfflineException.RETRY_AFTER_SECONDS);

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.RETRY_AFTER, String.valueOf(ServiceOfflineException.RETRY_AFTER_SECONDS));
        return new ResponseEntity<>(body, headers, HttpStatus.SERVICE_UNAVAILABLE);
    }

    private ResponseEntity<Map<String, Object>> buildErrorResponse(HttpStatus status, String message) {
        return new ResponseEntity<>(errorBody(status, message), status);
    }

    private Map<String, Object> errorBody(HttpStatus status, String message) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("status", status.value());
        response.put("error", status.getReasonPhrase());
        response.put("message", message);
        return response;
    }
}