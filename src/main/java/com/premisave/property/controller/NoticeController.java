package com.premisave.property.controller;

import com.premisave.property.dto.request.LeaseNoticeRequest;
import com.premisave.property.dto.request.ScheduleNoticeRequest;
import com.premisave.property.dto.request.UnitNoticeRequest;
import com.premisave.property.dto.response.NoticeResponse;
import com.premisave.property.dto.response.ScheduledNoticeResponse;
import com.premisave.property.enums.NoticeType;
import com.premisave.property.security.AccessGuard;
import com.premisave.property.service.NoticeSchedulingService;
import com.premisave.property.service.NoticeService;
import com.premisave.property.service.OwnerService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/notices")
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeService noticeService;
    private final NoticeSchedulingService noticeSchedulingService;
    private final OwnerService ownerService;
    private final AccessGuard guard;

    // ------------------------------------------------------------------
    // Single-recipient sends
    // ------------------------------------------------------------------

    // For tenants occupying a rental unit directly, with no lease.
    @PostMapping("/unit")
    public ResponseEntity<NoticeResponse> sendUnitNotice(@Valid @RequestBody UnitNoticeRequest request) {
        guard.requireUnitManager(request.getRentalUnitId());
        return ResponseEntity.ok(noticeService.sendUnitNotice(request));
    }

    // For tenants under an active/renewed lease.
    @PostMapping("/lease")
    public ResponseEntity<NoticeResponse> sendLeaseNotice(@Valid @RequestBody LeaseNoticeRequest request) {
        guard.requireLeaseManager(request.getLeaseId());
        return ResponseEntity.ok(noticeService.sendLeaseNotice(request));
    }

    // ------------------------------------------------------------------
    // Bulk / scheduled sends — home owner only. Set scheduledAt to a future
    // timestamp to queue it, omit it (or use a past/current time) to send
    // instantly. Either way you get a job id back to check status/results.
    // ------------------------------------------------------------------

    @PostMapping("/bulk")
    @PreAuthorize("hasRole('HOME_OWNER')")
    public ResponseEntity<ScheduledNoticeResponse> scheduleOrSendBulkNotice(
            @Valid @RequestBody ScheduleNoticeRequest request, HttpServletRequest httpRequest) {
        String ownerId = resolveOwnerId(httpRequest);
        return ResponseEntity.ok(noticeSchedulingService.scheduleOrSend(request, ownerId));
    }

    @GetMapping("/bulk/mine")
    @PreAuthorize("hasRole('HOME_OWNER')")
    public ResponseEntity<List<ScheduledNoticeResponse>> getMyBulkNotices(HttpServletRequest httpRequest) {
        String ownerId = resolveOwnerId(httpRequest);
        return ResponseEntity.ok(noticeSchedulingService.listForOwner(ownerId));
    }

    @GetMapping("/bulk/{id}")
    @PreAuthorize("hasRole('HOME_OWNER')")
    public ResponseEntity<ScheduledNoticeResponse> getBulkNotice(@PathVariable String id,
                                                                   HttpServletRequest httpRequest) {
        String ownerId = resolveOwnerId(httpRequest);
        return ResponseEntity.ok(noticeSchedulingService.get(id, ownerId));
    }

    @PostMapping("/bulk/{id}/cancel")
    @PreAuthorize("hasRole('HOME_OWNER')")
    public ResponseEntity<ScheduledNoticeResponse> cancelBulkNotice(@PathVariable String id,
                                                                      HttpServletRequest httpRequest) {
        String ownerId = resolveOwnerId(httpRequest);
        return ResponseEntity.ok(noticeSchedulingService.cancel(id, ownerId));
    }

    // ------------------------------------------------------------------
    // Reads
    // ------------------------------------------------------------------

    @GetMapping("/{id}")
    public ResponseEntity<NoticeResponse> getNotice(@PathVariable String id) {
        guard.requireNoticeViewer(id);
        return ResponseEntity.ok(noticeService.getNotice(id));
    }

    @GetMapping("/tenant/{tenantId}")
    public ResponseEntity<List<NoticeResponse>> getNoticesByTenant(@PathVariable String tenantId) {
        guard.requireTenantSelfOrLandlord(tenantId);
        return ResponseEntity.ok(visibleTo(tenantId, noticeService.getNoticesByTenant(tenantId)));
    }

    @GetMapping("/lease/{leaseId}")
    public ResponseEntity<List<NoticeResponse>> getNoticesByLease(@PathVariable String leaseId) {
        guard.requireLeaseParticipant(leaseId);
        return ResponseEntity.ok(noticeService.getNoticesByLease(leaseId));
    }

    @GetMapping("/tenant/{tenantId}/type/{noticeType}")
    public ResponseEntity<List<NoticeResponse>> getNoticesByTenantAndType(
            @PathVariable String tenantId, @PathVariable NoticeType noticeType) {
        guard.requireTenantSelfOrLandlord(tenantId);
        return ResponseEntity.ok(visibleTo(tenantId, noticeService.getNoticesByTenantAndType(tenantId, noticeType)));
    }

    // The tenant sees every notice sent to them. A landlord sees only the ones for their own properties.
    private List<NoticeResponse> visibleTo(String tenantId, List<NoticeResponse> notices) {
        if (guard.seesAllOf(tenantId)) {
            return notices;
        }
        Set<String> propertyIds = guard.ownedPropertyIds();
        return notices.stream().filter(n -> propertyIds.contains(n.getPropertyId())).toList();
    }

    private String resolveOwnerId(HttpServletRequest httpRequest) {
        String userId = (String) httpRequest.getAttribute("userId");
        return ownerService.getOwnerByUserId(userId).getId();
    }
}