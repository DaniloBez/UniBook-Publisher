package com.unibook.publisher.production.service;

import com.unibook.publisher.common.exception.notfound.ManuscriptNotFoundException;
import com.unibook.publisher.common.logging.AppLogger;
import com.unibook.publisher.production.entity.Manuscript;
import com.unibook.publisher.production.entity.ManuscriptAuditLog;
import com.unibook.publisher.production.enums.ManuscriptStatus;
import com.unibook.publisher.production.entity.response.ManuscriptAuditLogResponse;
import com.unibook.publisher.production.repository.ManuscriptAuditLogRepository;
import com.unibook.publisher.production.repository.ManuscriptRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ManuscriptAuditLogServiceImpl implements ManuscriptAuditLogService {
    private final ManuscriptAuditLogRepository manuscriptAuditLogRepository;
    private final ManuscriptRepository manuscriptRepository;
    private final AppLogger logger;

    public ManuscriptAuditLogServiceImpl(ManuscriptAuditLogRepository manuscriptAuditLogRepository, ManuscriptRepository manuscriptRepository, AppLogger logger) {
        this.manuscriptAuditLogRepository = manuscriptAuditLogRepository;
        this.manuscriptRepository = manuscriptRepository;
        this.logger = logger;
    }

    @Override
    @Transactional
    public void recordStatusChange(UUID manuscriptId, UUID changedByUserId, ManuscriptStatus oldStatus, ManuscriptStatus newStatus) {
        Manuscript manuscript = manuscriptRepository.findById(manuscriptId)
                .orElseThrow(() -> new ManuscriptNotFoundException(manuscriptId));

        ManuscriptAuditLog log = new ManuscriptAuditLog(
                null,
                manuscript,
                changedByUserId,
                oldStatus,
                newStatus,
                Instant.now()
        );
        manuscriptAuditLogRepository.save(log);

        logger.info(
                "Recorded audit log for manuscript {}: status changed {} -> {} by user {}",
                manuscriptId,
                oldStatus,
                newStatus,
                changedByUserId
        );
    }

    @Override
    public List<ManuscriptAuditLogResponse> getAuditLog(UUID manuscriptId) {
        if (!manuscriptRepository.existsById(manuscriptId)) {
            throw new ManuscriptNotFoundException(manuscriptId);
        }
        return manuscriptAuditLogRepository.findByManuscriptId(manuscriptId).stream()
                .map(ManuscriptAuditLogResponse::from)
                .toList();
    }
}
