package com.unibook.publisher.production.entity;

import java.time.Instant;
import java.util.UUID;

public record Revision(
      UUID revisionId,
      UUID chapterId,
      int versionNumber,
      String fileUrl,
      UUID uploadedByUserId,
      Instant uploadedAt
) {}
