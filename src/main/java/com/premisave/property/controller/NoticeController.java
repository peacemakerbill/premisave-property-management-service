package com.premisave.property.controller;

import com.premisave.property.dto.request.LeaseNoticeRequest;
import com.premisave.property.dto.request.UnitNoticeRequest;
import com.premisave.property.dto.response.NoticeResponse;
import com.premisave.property.enums.NoticeType;
import com.premisave.property.service.NoticeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notices")
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeService noticeService;

    // For tenants occupying a rental unit directly, with no lease.
    @PostMapping("/unit")
    public ResponseEntity<NoticeResponse> sendUnitNotice(@Valid @RequestBody UnitNoticeRequest request) {
        return ResponseEntity.ok(noticeService.sendUnitNotice(request));
    }

    // For tenants under an active/renewed lease.
    @PostMapping("/lease")
    public ResponseEntity<NoticeResponse> sendLeaseNotice(@Valid @RequestBody LeaseNoticeRequest request) {
        return ResponseEntity.ok(noticeService.sendLeaseNotice(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<NoticeResponse> getNotice(@PathVariable String id) {
        return ResponseEntity.ok(noticeService.getNotice(id));
    }

    @GetMapping("/tenant/{tenantId}")
    public ResponseEntity<List<NoticeResponse>> getNoticesByTenant(@PathVariable String tenantId) {
        return ResponseEntity.ok(noticeService.getNoticesByTenant(tenantId));
    }

    @GetMapping("/lease/{leaseId}")
    public ResponseEntity<List<NoticeResponse>> getNoticesByLease(@PathVariable String leaseId) {
        return ResponseEntity.ok(noticeService.getNoticesByLease(leaseId));
    }

    @GetMapping("/tenant/{tenantId}/type/{noticeType}")
    public ResponseEntity<List<NoticeResponse>> getNoticesByTenantAndType(
            @PathVariable String tenantId, @PathVariable NoticeType noticeType) {
        return ResponseEntity.ok(noticeService.getNoticesByTenantAndType(tenantId, noticeType));
    }
}