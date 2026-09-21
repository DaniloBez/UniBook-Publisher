package com.unibook.publisher.production.service;

import com.unibook.publisher.common.logging.AppLogger;
import com.unibook.publisher.production.entity.ManuscriptAuditLog;
import com.unibook.publisher.production.enums.ManuscriptStatus;
import com.unibook.publisher.production.entity.response.AuditLogResponse;
import com.unibook.publisher.production.repository.ManuscriptAuditLogRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class AuditLogService {
    private final ManuscriptAuditLogRepository auditLogRepository;
    private final AppLogger logger;

    public AuditLogService(ManuscriptAuditLogRepository auditLogRepository, AppLogger logger) {
        this.auditLogRepository = auditLogRepository;
        this.logger = logger;
    }

    public void record(UUID manuscriptId, UUID changedByUserId, ManuscriptStatus oldStatus, ManuscriptStatus newStatus) {
        auditLogRepository.save(new ManuscriptAuditLog(
                UUID.randomUUID(),
                manuscriptId,
                changedByUserId,
                oldStatus,
                newStatus,
                Instant.now()
        ));
    }

    public List<AuditLogResponse> getAuditLog(UUID manuscriptId) {
        return auditLogRepository.findByManuscriptId(manuscriptId).stream()
                .map(AuditLogResponse::from)
                .toList();
    }
}
