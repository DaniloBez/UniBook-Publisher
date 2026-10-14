package com.unibook.publisher.production.service;

import com.unibook.publisher.identity.entity.User;
import com.unibook.publisher.production.entity.request.AssignDesignerRequest;
import com.unibook.publisher.production.entity.response.ManuscriptAuditLogResponse;
import com.unibook.publisher.production.entity.response.ManuscriptResponse;

import java.util.List;
import java.util.UUID;

public interface ManuscriptFinalizationService {
    ManuscriptResponse finalizeText(UUID manuscriptId, User currentUser);
    ManuscriptResponse assignDesigner(UUID manuscriptId, User currentUser, AssignDesignerRequest request);
    ManuscriptResponse publish(UUID manuscriptId, User currentUser);
    List<ManuscriptAuditLogResponse> getAuditLog(UUID manuscriptId);
}
