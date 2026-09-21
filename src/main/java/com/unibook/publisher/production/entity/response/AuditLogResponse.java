package com.unibook.publisher.production.entity.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.unibook.publisher.production.entity.ManuscriptAuditLog;
import com.unibook.publisher.production.enums.ManuscriptStatus;

import java.time.Instant;
import java.util.UUID;

public record AuditLogResponse(
        UUID id,

        @JsonProperty("manuscript_id")
        UUID manuscriptId,

        @JsonProperty("changed_by_user_id")
        UUID changedByUserId,

        @JsonProperty("old_status")
        ManuscriptStatus oldStatus,

        @JsonProperty("new_status")
        ManuscriptStatus newStatus,

        Instant timestamp
) {
    public static AuditLogResponse from(ManuscriptAuditLog log) {
        return new AuditLogResponse(
                log.id(),
                log.manuscriptId(),
                log.changedByUserId(),
                log.oldStatus(),
                log.newStatus(),
                log.timestamp()
        );
    }
}
