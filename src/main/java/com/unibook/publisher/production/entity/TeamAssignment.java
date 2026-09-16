package com.unibook.publisher.production.entity;

import com.unibook.publisher.common.enums.UserRole;

import java.time.Instant;
import java.util.UUID;

public record TeamAssignment(
     UUID teamId,
     UUID manuscriptId,
     UUID userId,
     UserRole role, //дизайнер або редактор
     Instant assignedAt
) {}
