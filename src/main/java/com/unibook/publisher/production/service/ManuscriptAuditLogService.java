package com.unibook.publisher.production.service;

import com.unibook.publisher.production.enums.ManuscriptStatus;
import com.unibook.publisher.production.entity.response.ManuscriptAuditLogResponse;

import java.util.List;
import java.util.UUID;

public interface ManuscriptAuditLogService {
    void record(UUID manuscriptId, UUID changedByUserId, ManuscriptStatus oldStatus, ManuscriptStatus newStatus);
    List<ManuscriptAuditLogResponse> getAuditLog(UUID manuscriptId);
}
