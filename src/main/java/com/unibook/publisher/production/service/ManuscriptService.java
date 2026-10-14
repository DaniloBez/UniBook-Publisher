package com.unibook.publisher.production.service;

import com.unibook.publisher.identity.entity.User;
import com.unibook.publisher.production.entity.request.*;
import com.unibook.publisher.production.entity.response.ManuscriptResponse;
import com.unibook.publisher.production.enums.ManuscriptStatus;

import java.util.List;
import java.util.UUID;

public interface ManuscriptService {
    ManuscriptResponse getManuscriptById(UUID id);
    List<ManuscriptResponse> getManuscripts(ManuscriptStatus status);
    ManuscriptResponse approveManuscript(UUID id, User currentUser, ManuscriptApprovalRequest request);
    ManuscriptResponse rejectManuscript(UUID id, User currentUser, ManuscriptRejectionRequest request);
    ManuscriptResponse postponeManuscript(UUID id, User currentUser, ManuscriptPostponementRequest request);
    ManuscriptResponse submitManuscript(User currentUser, ManuscriptSubmissionRequest request);
    ManuscriptResponse updateManuscript(UUID id, ManuscriptUpdateRequest request);
    void deleteManuscript(UUID id);
}
