package com.premisave.property.client;

import com.premisave.property.dto.response.UserDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "auth-service", url = "${auth-service.url}")
public interface AuthServiceClient {

    @GetMapping("/internal/users/{userId}")
    UserDto getUserById(@PathVariable String userId,
                        @RequestHeader("X-API-Key") String apiKey);

    @GetMapping("/internal/users/email/{email}")
    UserDto getUserByEmail(@PathVariable String email,
                           @RequestHeader("X-API-Key") String apiKey);
}