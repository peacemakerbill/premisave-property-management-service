package com.premisave.property.service;

import com.premisave.property.dto.request.NoticeRequest;
import com.premisave.property.entity.Notice;
import com.premisave.property.repository.NoticeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NoticeService {

    private final NoticeRepository noticeRepository;

    public void sendNotice(NoticeRequest request) {
        Notice notice = new Notice();
        // map fields
        noticeRepository.save(notice);
    }
}