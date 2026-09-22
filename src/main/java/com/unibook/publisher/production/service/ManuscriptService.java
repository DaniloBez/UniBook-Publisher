package com.unibook.publisher.production.service;

import com.unibook.publisher.production.entity.request.ManuscriptApprovalRequest;
import com.unibook.publisher.production.entity.request.ManuscriptPostponementRequest;
import com.unibook.publisher.production.entity.request.ManuscriptRejectionRequest;
import com.unibook.publisher.production.entity.request.ManuscriptSubmissionRequest;
import com.unibook.publisher.production.entity.response.ManuscriptResponse;
import com.unibook.publisher.production.enums.ManuscriptStatus;

import java.util.List;
import java.util.UUID;

public interface ManuscriptService {

    ManuscriptResponse getManuscriptById(UUID id);

    List<ManuscriptResponse> getManuscripts(ManuscriptStatus status);

    ManuscriptResponse approveManuscript(UUID id, UUID chiefEditorId, ManuscriptApprovalRequest request);

    ManuscriptResponse rejectManuscript(UUID id, UUID chiefEditorId, ManuscriptRejectionRequest request);

    ManuscriptResponse postponeManuscript(UUID id, UUID chiefEditorId, ManuscriptPostponementRequest request);

    ManuscriptResponse submitManuscript(UUID authorId, ManuscriptSubmissionRequest request);
}
