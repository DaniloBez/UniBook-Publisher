package com.unibook.publisher.production.service;

import com.unibook.publisher.common.enums.UserRole;
import com.unibook.publisher.production.entity.response.TeamAssignmentResponse;

import java.util.UUID;
import java.util.List;

public interface TeamAssignmentService {
    List<UUID> getManuscriptIdsForUser(UUID userId, UserRole role);
    TeamAssignmentResponse assign(UUID manuscriptId, UUID userId, UserRole role);
}
