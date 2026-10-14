package com.unibook.publisher.production.service;

import com.unibook.publisher.common.security.AuthenticatedUser;
import com.unibook.publisher.production.entity.request.AssignDesignerRequest;
import com.unibook.publisher.production.entity.response.ManuscriptAuditLogResponse;
import com.unibook.publisher.production.entity.response.ManuscriptResponse;

import java.util.List;
import java.util.UUID;

public interface ManuscriptFinalizationService {
    ManuscriptResponse finalizeText(UUID manuscriptId, AuthenticatedUser currentUser);
    ManuscriptResponse assignDesigner(UUID manuscriptId, AuthenticatedUser currentUser, AssignDesignerRequest request);
    ManuscriptResponse publish(UUID manuscriptId, AuthenticatedUser currentUser);
    List<ManuscriptAuditLogResponse> getAuditLog(UUID manuscriptId);
}
