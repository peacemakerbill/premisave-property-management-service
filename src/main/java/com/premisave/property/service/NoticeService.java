package com.premisave.property.service;

import com.premisave.property.dto.request.NoticeRequest;
import com.premisave.property.dto.response.NoticeResponse;
import com.premisave.property.entity.Notice;
import com.premisave.property.enums.NoticeType;
import com.premisave.property.exception.ResourceNotFoundException;
import com.premisave.property.repository.NoticeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NoticeService {

    private final NoticeRepository noticeRepository;

    @Transactional
    public NoticeResponse sendNotice(NoticeRequest request) {
        Notice notice = new Notice();
        notice.setTenantId(request.getTenantId());
        notice.setLeaseId(request.getLeaseId());
        notice.setNoticeType(request.getNoticeType());
        notice.setTitle(request.getTitle());
        notice.setContent(request.getContent());
        notice.setSentAt(LocalDateTime.now());

        return toResponse(noticeRepository.save(notice));
    }

    public NoticeResponse getNotice(String id) {
        return toResponse(findOrThrow(id));
    }

    public List<NoticeResponse> getNoticesByTenant(String tenantId) {
        return noticeRepository.findByTenantId(tenantId).stream()
                .map(this::toResponse)
                .toList();
    }

    public List<NoticeResponse> getNoticesByLease(String leaseId) {
        return noticeRepository.findByLeaseId(leaseId).stream()
                .map(this::toResponse)
                .toList();
    }

    public List<NoticeResponse> getNoticesByTenantAndType(String tenantId, NoticeType noticeType) {
        return noticeRepository.findByTenantIdAndNoticeType(tenantId, noticeType).stream()
                .map(this::toResponse)
                .toList();
    }

    private Notice findOrThrow(String id) {
        return noticeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notice not found"));
    }

    private NoticeResponse toResponse(Notice notice) {
        NoticeResponse response = new NoticeResponse();
        response.setId(notice.getId());
        response.setTenantId(notice.getTenantId());
        response.setLeaseId(notice.getLeaseId());
        response.setNoticeType(notice.getNoticeType());
        response.setTitle(notice.getTitle());
        response.setContent(notice.getContent());
        response.setSentAt(notice.getSentAt());
        return response;
    }
}