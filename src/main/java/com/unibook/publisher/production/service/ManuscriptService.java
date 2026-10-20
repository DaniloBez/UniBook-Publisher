package com.unibook.publisher.production.service;

import com.unibook.publisher.common.enums.UserRole;
import com.unibook.publisher.common.security.AuthenticatedUser;
import com.unibook.publisher.production.entity.request.*;
import com.unibook.publisher.production.entity.response.ManuscriptResponse;
import com.unibook.publisher.production.enums.ManuscriptStatus;

import java.util.List;
import java.util.UUID;

public interface ManuscriptService {
    ManuscriptResponse getManuscriptById(UUID id);
    List<ManuscriptResponse> getManuscripts(ManuscriptStatus status);
    List<ManuscriptResponse> getManuscriptsForUser(UUID userId, UserRole role);
    ManuscriptResponse approveManuscript(UUID id, AuthenticatedUser currentUser, ManuscriptApprovalRequest request);
    ManuscriptResponse rejectManuscript(UUID id, AuthenticatedUser currentUser, ManuscriptRejectionRequest request);
    ManuscriptResponse postponeManuscript(UUID id, AuthenticatedUser currentUser, ManuscriptPostponementRequest request);
    ManuscriptResponse submitManuscript(AuthenticatedUser currentUser, ManuscriptSubmissionRequest request);
    ManuscriptResponse updateManuscript(UUID id, ManuscriptUpdateRequest request);
    void deleteManuscript(UUID id);
}
