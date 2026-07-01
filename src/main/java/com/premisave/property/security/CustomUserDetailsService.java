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
        try {
            // TODO: In production, call Auth Service via Feign
            // UserDto user = authServiceClient.getUserByEmail(email);

            // Temporary implementation
            return new UserPrincipal(
                email,
                "temp-user-id",           // Replace with real userId from Auth
                "HOME_OWNER"              // Replace with actual role from Auth
            );

        } catch (Exception e) {
            throw new UsernameNotFoundException("User not found with email: " + email, e);
        }
    }
}