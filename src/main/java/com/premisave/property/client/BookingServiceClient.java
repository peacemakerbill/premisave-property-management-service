package com.premisave.property.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "booking-service", url = "${booking-service.url:}")
public interface BookingServiceClient {

    @GetMapping("/internal/units/{unitId}/availability")
    Boolean isUnitAvailable(@PathVariable String unitId,
                            @RequestHeader("X-API-Key") String apiKey);
}