package com.premisave.property.controller;

import com.premisave.property.dto.request.NoticeRequest;
import com.premisave.property.service.NoticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/notices")
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeService noticeService;

    @PostMapping
    public ResponseEntity<String> sendNotice(@RequestBody NoticeRequest request) {
        noticeService.sendNotice(request);
        return ResponseEntity.ok("Notice sent successfully");
    }
}