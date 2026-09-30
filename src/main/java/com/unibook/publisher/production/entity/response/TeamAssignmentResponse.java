package com.unibook.publisher.production.entity.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.unibook.publisher.common.enums.UserRole;
import com.unibook.publisher.production.entity.TeamAssignment;

import java.time.Instant;
import java.util.UUID;

public record TeamAssignmentResponse(
        @JsonProperty("assignment_id")
        UUID assignmentId,

        @JsonProperty("manuscript_id")
        UUID manuscriptId,

        @JsonProperty("user_id")
        UUID userId,

        UserRole role,

        @JsonProperty("assigned_at")
        Instant assignedAt
) {
    public static TeamAssignmentResponse from(TeamAssignment assignment) {
        UUID manuscriptId = (assignment.getManuscript() != null) ? assignment.getManuscript().getManuscriptId() : null;
        return new TeamAssignmentResponse(
                assignment.getAssignmentId(),
                manuscriptId,
                assignment.getUserId(),
                assignment.getRole(),
                assignment.getAssignedAt()
        );
    }
}
