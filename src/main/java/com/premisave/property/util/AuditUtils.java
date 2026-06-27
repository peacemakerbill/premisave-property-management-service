package com.premisave.property.util;

import com.premisave.property.entity.AuditLog;
import com.premisave.property.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuditUtils {

    private final AuditLogRepository auditLogRepository;

    public void logAction(String entityType, String entityId, String action, String performedBy, String description) {
        AuditLog log = new AuditLog();
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setAction(action);
        log.setPerformedBy(performedBy);
        log.setDescription(description);
        auditLogRepository.save(log);
    }
}