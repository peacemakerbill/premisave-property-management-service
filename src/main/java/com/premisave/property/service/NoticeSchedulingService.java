package com.premisave.property.service;

import com.premisave.property.dto.request.LeaseNoticeRequest;
import com.premisave.property.dto.request.ScheduleNoticeRequest;
import com.premisave.property.dto.request.UnitNoticeRequest;
import com.premisave.property.dto.response.NoticeDeliveryResultResponse;
import com.premisave.property.dto.response.NoticeResponse;
import com.premisave.property.dto.response.ScheduledNoticeResponse;
import com.premisave.property.entity.Lease;
import com.premisave.property.entity.NoticeDeliveryResult;
import com.premisave.property.entity.Property;
import com.premisave.property.entity.RentalUnit;
import com.premisave.property.entity.ScheduledNotice;
import com.premisave.property.enums.NotificationChannel;
import com.premisave.property.enums.RecipientType;
import com.premisave.property.enums.ScheduledNoticeStatus;
import com.premisave.property.exception.BadRequestException;
import com.premisave.property.exception.ResourceNotFoundException;
import com.premisave.property.exception.UnauthorizedException;
import com.premisave.property.repository.LeaseRepository;
import com.premisave.property.repository.PropertyRepository;
import com.premisave.property.repository.RentalUnitRepository;
import com.premisave.property.repository.ScheduledNoticeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NoticeSchedulingService {

    private final ScheduledNoticeRepository scheduledNoticeRepository;
    private final RentalUnitRepository rentalUnitRepository;
    private final LeaseRepository leaseRepository;
    private final PropertyRepository propertyRepository;
    private final NoticeService noticeService;
    private final EmailService emailService;
    private final SmsService smsService;

    // ------------------------------------------------------------------
    // Entry point: create a job, send instantly or leave PENDING for the
    // scheduler. Intentionally NOT wrapped in @Transactional at this level —
    // each recipient's notice is created in its own independent transaction
    // (see NoticeService), so one failing recipient in a batch of 50 never
    // rolls back the 49 that already succeeded.
    // ------------------------------------------------------------------

    public ScheduledNoticeResponse scheduleOrSend(ScheduleNoticeRequest request, String ownerId) {
        List<String> unitIds = request.getRentalUnitIds() != null ? request.getRentalUnitIds() : List.of();
        List<String> leaseIds = request.getLeaseIds() != null ? request.getLeaseIds() : List.of();

        if (unitIds.isEmpty() && leaseIds.isEmpty()) {
            throw new BadRequestException("Provide at least one rentalUnitId or leaseId to notify");
        }

        assertOwnership(unitIds, leaseIds, ownerId);

        boolean sendNow = request.getScheduledAt() == null || !request.getScheduledAt().isAfter(LocalDateTime.now());

        ScheduledNotice job = new ScheduledNotice();
        job.setCreatedByOwnerId(ownerId);
        job.setRentalUnitIds(unitIds);
        job.setLeaseIds(leaseIds);
        job.setNoticeType(request.getNoticeType());
        job.setTitle(request.getTitle());
        job.setContent(request.getContent());
        job.setChannels(request.getChannels());
        job.setScheduledAt(sendNow ? null : request.getScheduledAt());
        job.setStatus(sendNow ? ScheduledNoticeStatus.PROCESSING : ScheduledNoticeStatus.PENDING);

        ScheduledNotice saved = scheduledNoticeRepository.save(job);

        if (sendNow) {
            saved = process(saved);
        }

        return toResponse(saved);
    }

    // ------------------------------------------------------------------
    // Scheduler poller — runs every minute, dispatches any job whose
    // scheduledAt is now due. Mirrors RentScheduleService.markOverdueSchedules().
    // ------------------------------------------------------------------

    @Scheduled(cron = "0 * * * * *")
    public void dispatchDueScheduledNotices() {
        List<ScheduledNotice> due = scheduledNoticeRepository
                .findByStatusAndScheduledAtLessThanEqual(ScheduledNoticeStatus.PENDING, LocalDateTime.now());

        for (ScheduledNotice job : due) {
            job.setStatus(ScheduledNoticeStatus.PROCESSING);
            scheduledNoticeRepository.save(job);
            try {
                process(job);
            } catch (Exception e) {
                log.error("Failed to process scheduled notice job {}: {}", job.getId(), e.getMessage());
                job.setStatus(ScheduledNoticeStatus.FAILED);
                job.setProcessedAt(LocalDateTime.now());
                scheduledNoticeRepository.save(job);
            }
        }
    }

    // ------------------------------------------------------------------
    // Processing — creates a Notice per recipient (reusing NoticeService's
    // existing validation: tenant/lease/occupancy checks, 24h duplicate
    // guard), dispatches configured channels, and records a per-recipient
    // result. One recipient's failure does not affect the others.
    // ------------------------------------------------------------------

    private ScheduledNotice process(ScheduledNotice job) {
        List<NoticeDeliveryResult> results = new ArrayList<>();

        for (String unitId : safe(job.getRentalUnitIds())) {
            results.add(processUnitRecipient(unitId, job));
        }
        for (String leaseId : safe(job.getLeaseIds())) {
            results.add(processLeaseRecipient(leaseId, job));
        }

        long successCount = results.stream().filter(NoticeDeliveryResult::isSuccess).count();
        ScheduledNoticeStatus finalStatus;
        if (results.isEmpty() || successCount == 0) {
            finalStatus = ScheduledNoticeStatus.FAILED;
        } else if (successCount == results.size()) {
            finalStatus = ScheduledNoticeStatus.SENT;
        } else {
            finalStatus = ScheduledNoticeStatus.PARTIALLY_SENT;
        }

        job.setResults(results);
        job.setStatus(finalStatus);
        job.setProcessedAt(LocalDateTime.now());

        return scheduledNoticeRepository.save(job);
    }

    private NoticeDeliveryResult processUnitRecipient(String unitId, ScheduledNotice job) {
        NoticeDeliveryResult result = new NoticeDeliveryResult();
        result.setRecipientType(RecipientType.UNIT);
        result.setRecipientId(unitId);

        try {
            UnitNoticeRequest request = new UnitNoticeRequest();
            request.setRentalUnitId(unitId);
            request.setNoticeType(job.getNoticeType());
            request.setTitle(job.getTitle());
            request.setContent(job.getContent());

            NoticeResponse notice = noticeService.sendUnitNotice(request);
            populateDelivery(result, notice, job.getChannels());
        } catch (RuntimeException e) {
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
        }
        return result;
    }

    private NoticeDeliveryResult processLeaseRecipient(String leaseId, ScheduledNotice job) {
        NoticeDeliveryResult result = new NoticeDeliveryResult();
        result.setRecipientType(RecipientType.LEASE);
        result.setRecipientId(leaseId);

        try {
            LeaseNoticeRequest request = new LeaseNoticeRequest();
            request.setLeaseId(leaseId);
            request.setNoticeType(job.getNoticeType());
            request.setTitle(job.getTitle());
            request.setContent(job.getContent());

            NoticeResponse notice = noticeService.sendLeaseNotice(request);
            populateDelivery(result, notice, job.getChannels());
        } catch (RuntimeException e) {
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
        }
        return result;
    }

    private void populateDelivery(NoticeDeliveryResult result, NoticeResponse notice, List<NotificationChannel> channels) {
        result.setSuccess(true);
        result.setNoticeId(notice.getId());
        result.setTenantId(notice.getTenantId());

        if (channels.contains(NotificationChannel.EMAIL)) {
            String email = notice.getTenant() != null ? notice.getTenant().getEmail() : null;
            result.setEmailSent(emailService.sendNoticeEmail(email, notice.getTitle(), notice.getContent()));
        }

        if (channels.contains(NotificationChannel.SMS)) {
            result.setSmsRequested(true);
            String phone = notice.getTenant() != null ? notice.getTenant().getPhoneNumber() : null;
            smsService.sendNoticeSms(phone, notice.getTitle() + ": " + notice.getContent()); // TODO: no-op today
        }
    }

    // ------------------------------------------------------------------
    // Ownership guard — a home owner may only notify tenants on their own
    // properties. This is an authorization check, so it fails the whole
    // request rather than silently skipping the offending recipient.
    // ------------------------------------------------------------------

    private void assertOwnership(List<String> unitIds, List<String> leaseIds, String ownerId) {
        for (String unitId : unitIds) {
            RentalUnit unit = rentalUnitRepository.findById(unitId)
                    .orElseThrow(() -> new ResourceNotFoundException("Rental unit not found: " + unitId));
            assertPropertyOwnedBy(unit.getPropertyId(), ownerId);
        }
        for (String leaseId : leaseIds) {
            Lease lease = leaseRepository.findById(leaseId)
                    .orElseThrow(() -> new ResourceNotFoundException("Lease not found: " + leaseId));
            assertPropertyOwnedBy(lease.getPropertyId(), ownerId);
        }
    }

    private void assertPropertyOwnedBy(String propertyId, String ownerId) {
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new ResourceNotFoundException("Property not found: " + propertyId));
        if (!property.getOwnerId().equals(ownerId)) {
            throw new UnauthorizedException("You do not have access to one or more of the selected units/leases");
        }
    }

    // ------------------------------------------------------------------
    // Reads
    // ------------------------------------------------------------------

    public ScheduledNoticeResponse get(String id, String ownerId) {
        ScheduledNotice job = findOrThrow(id);
        assertJobOwnedBy(job, ownerId);
        return toResponse(job);
    }

    public List<ScheduledNoticeResponse> listForOwner(String ownerId) {
        return scheduledNoticeRepository.findByCreatedByOwnerId(ownerId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public ScheduledNoticeResponse cancel(String id, String ownerId) {
        ScheduledNotice job = findOrThrow(id);
        assertJobOwnedBy(job, ownerId);

        if (job.getStatus() != ScheduledNoticeStatus.PENDING) {
            throw new BadRequestException("Only a PENDING scheduled notice can be cancelled");
        }

        job.setStatus(ScheduledNoticeStatus.CANCELLED);
        return toResponse(scheduledNoticeRepository.save(job));
    }

    private void assertJobOwnedBy(ScheduledNotice job, String ownerId) {
        if (!job.getCreatedByOwnerId().equals(ownerId)) {
            throw new UnauthorizedException("You do not have access to this scheduled notice");
        }
    }

    private ScheduledNotice findOrThrow(String id) {
        return scheduledNoticeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Scheduled notice not found"));
    }

    private List<String> safe(List<String> list) {
        return list != null ? list : List.of();
    }

    // ------------------------------------------------------------------
    // Mapping
    // ------------------------------------------------------------------

    private ScheduledNoticeResponse toResponse(ScheduledNotice job) {
        ScheduledNoticeResponse response = new ScheduledNoticeResponse();
        response.setId(job.getId());
        response.setStatus(job.getStatus());
        response.setNoticeType(job.getNoticeType());
        response.setTitle(job.getTitle());
        response.setChannels(job.getChannels());
        response.setScheduledAt(job.getScheduledAt());
        response.setProcessedAt(job.getProcessedAt());
        response.setCreatedAt(job.getCreatedAt());

        List<NoticeDeliveryResult> results = job.getResults() != null ? job.getResults() : List.of();
        response.setTotalRecipients(
                (job.getRentalUnitIds() != null ? job.getRentalUnitIds().size() : 0)
                        + (job.getLeaseIds() != null ? job.getLeaseIds().size() : 0));
        response.setSuccessCount((int) results.stream().filter(NoticeDeliveryResult::isSuccess).count());
        response.setFailureCount((int) results.stream().filter(r -> !r.isSuccess()).count());
        response.setResults(results.stream().map(this::toResultResponse).toList());

        return response;
    }

    private NoticeDeliveryResultResponse toResultResponse(NoticeDeliveryResult result) {
        NoticeDeliveryResultResponse response = new NoticeDeliveryResultResponse();
        response.setRecipientType(result.getRecipientType());
        response.setRecipientId(result.getRecipientId());
        response.setTenantId(result.getTenantId());
        response.setNoticeId(result.getNoticeId());
        response.setSuccess(result.isSuccess());
        response.setErrorMessage(result.getErrorMessage());
        response.setEmailSent(result.isEmailSent());
        response.setSmsRequested(result.isSmsRequested());
        return response;
    }
}