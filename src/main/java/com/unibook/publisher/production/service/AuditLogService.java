package com.unibook.publisher.production.service;

import com.unibook.publisher.production.enums.ManuscriptStatus;
import com.unibook.publisher.production.entity.response.AuditLogResponse;

import java.util.List;
import java.util.UUID;

public interface AuditLogService {

    void record(UUID manuscriptId, UUID changedByUserId, ManuscriptStatus oldStatus, ManuscriptStatus newStatus);

    List<AuditLogResponse> getAuditLog(UUID manuscriptId);
}
