package com.premisave.property.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "listing-service", url = "${listing-service.url:}")
public interface ListingServiceClient {

    @PostMapping("/internal/properties/sync")
    void syncPropertyToListing(@RequestBody Object propertyData,
                               @RequestHeader("X-API-Key") String apiKey);
}