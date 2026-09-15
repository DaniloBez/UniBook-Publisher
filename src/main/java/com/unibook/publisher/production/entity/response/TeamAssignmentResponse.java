package com.unibook.publisher.production.entity.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.unibook.publisher.common.enums.UserRole;
import com.unibook.publisher.production.entity.TeamAssignment;

import java.time.Instant;
import java.util.UUID;

public record TeamAssignmentResponse(
        @JsonProperty("team_id")
        UUID teamId,

        @JsonProperty("manuscript_id")
        UUID manuscriptId,

        @JsonProperty("user_id")
        UUID userId,

        UserRole role,

        @JsonProperty("assigned_at")
        Instant assignedAt
) {
    public static TeamAssignmentResponse from(TeamAssignment teamAssignment) {
        return new TeamAssignmentResponse(
                teamAssignment.teamId(),
                teamAssignment.manuscriptId(),
                teamAssignment.userId(),
                teamAssignment.role(),
                teamAssignment.assignedAt()
        );
    }
}
