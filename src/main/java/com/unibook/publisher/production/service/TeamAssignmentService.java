package com.unibook.publisher.production.service;

import com.unibook.publisher.common.enums.UserRole;
import com.unibook.publisher.production.entity.response.TeamAssignmentResponse;

import java.util.UUID;

public interface TeamAssignmentService {

    TeamAssignmentResponse assign(UUID manuscriptId, UUID userId, UserRole role);
}
