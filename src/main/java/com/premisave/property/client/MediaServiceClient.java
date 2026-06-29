package com.premisave.property.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

@FeignClient(name = "media-service", url = "${media-service.url:}")
public interface MediaServiceClient {

    @PostMapping("/upload/property")
    String uploadPropertyImage(@RequestParam("file") MultipartFile file,
                               @RequestHeader("X-API-Key") String apiKey);
}