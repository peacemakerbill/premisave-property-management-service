package com.premisave.property.security;

import com.premisave.property.client.AuthServiceClient;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final AuthServiceClient authServiceClient;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // Call Auth Service to get user details
        // For now, using a simple implementation. In production, use Feign client.

        try {
            // Example: Fetch from Auth Service via Feign
            // UserDto user = authServiceClient.getUserByEmail(email);

            return UserPrincipal.builder()
                    .email(email)
                    .userId("user-id-from-auth")  // populated from Auth Service
                    .role("HOME_OWNER")           // or CLIENT, ADMIN, etc.
                    .build();

        } catch (Exception e) {
            throw new UsernameNotFoundException("User not found with email: " + email, e);
        }
    }
}