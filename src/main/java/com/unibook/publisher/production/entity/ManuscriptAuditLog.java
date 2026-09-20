package com.unibook.publisher.production.entity;

import com.unibook.publisher.production.enums.ManuscriptStatus;

import java.time.Instant;
import java.util.UUID;

public record ManuscriptAuditLog(
    UUID id, 
    UUID manuscriptId, 
    UUID changedByUserId,
    ManuscriptStatus oldStatus,
    ManuscriptStatus newStatus, 
    Instant timestamp
) {}
