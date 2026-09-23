package com.unibook.publisher.production.service;

import com.unibook.publisher.production.entity.request.AssignDesignerRequest;
import com.unibook.publisher.production.entity.response.AuditLogResponse;
import com.unibook.publisher.production.entity.response.ManuscriptResponse;

import java.util.List;
import java.util.UUID;

public interface ManuscriptFinalizationService {

    ManuscriptResponse finalizeText(UUID manuscriptId, UUID editorId);

    ManuscriptResponse assignDesigner(UUID manuscriptId, UUID chiefEditorId, AssignDesignerRequest request);

    ManuscriptResponse publish(UUID manuscriptId, UUID chiefEditorId);

    List<AuditLogResponse> getAuditLog(UUID manuscriptId);
}
