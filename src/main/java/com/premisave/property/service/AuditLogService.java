package com.premisave.property.service;

import com.premisave.property.entity.AuditLog;
import com.premisave.property.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public void log(String entityType, String entityId, String action, String userId, String description) {
        AuditLog log = new AuditLog();
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setAction(action);
        log.setPerformedBy(userId);
        log.setDescription(description);
        auditLogRepository.save(log);
    }
}