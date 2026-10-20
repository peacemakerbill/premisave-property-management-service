package com.premisave.property.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Slf4j
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    /** Request attribute set when a bearer token was sent but rejected: EXPIRED or INVALID. */
    public static final String AUTH_ERROR = "authError";

    private final JwtService jwtService;

    public JwtAuthFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        try {
            if (jwtService.isTokenValid(token)) {
                String email = jwtService.extractEmail(token);
                String userId = jwtService.extractUserId(token);
                String role = jwtService.extractRole(token);

                List<GrantedAuthority> authorities = role != null
                        ? List.of(new SimpleGrantedAuthority("ROLE_" + role))
                        : List.of();

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(email, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(authentication);

                if (userId != null) {
                    request.setAttribute("userId", userId);
                }
            } else {
                request.setAttribute(AUTH_ERROR, "EXPIRED");
            }
        } catch (ExpiredJwtException e) {
            request.setAttribute(AUTH_ERROR, "EXPIRED");
            log.debug("Rejected an expired token for {} {}", request.getMethod(), request.getRequestURI());
        } catch (JwtException | IllegalArgumentException e) {
            // Bad signature, wrong secret, malformed token. Worth seeing in the logs, never the token itself.
            request.setAttribute(AUTH_ERROR, "INVALID");
            log.warn("Rejected an invalid token for {} {}: {}", request.getMethod(), request.getRequestURI(),
                    e.getClass().getSimpleName());
        }

        filterChain.doFilter(request, response);
    }
}
