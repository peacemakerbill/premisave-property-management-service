package com.premisave.property.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

/**
 * Answers a request that is not signed in with a 401 and a JSON body the frontend can act on,
 * instead of Spring Security's empty 403. code SESSION_EXPIRED tells the app to send the user
 * back to sign in; AUTHENTICATION_REQUIRED covers a missing or unusable token.
 */
@Component
public class JsonAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        boolean expired = "EXPIRED".equals(request.getAttribute(JwtAuthFilter.AUTH_ERROR));
        String code = expired ? "SESSION_EXPIRED" : "AUTHENTICATION_REQUIRED";
        String message = expired
                ? "Your session has expired. Please sign in again."
                : "Please sign in to continue.";

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"timestamp\":\"" + LocalDateTime.now() + "\","
                + "\"status\":401,\"error\":\"Unauthorized\","
                + "\"code\":\"" + code + "\",\"message\":\"" + message + "\"}");
    }
}
