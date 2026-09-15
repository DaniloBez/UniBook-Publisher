package com.unibook.publisher.production.entity;

import java.time.Instant;
import java.util.UUID;

public record CoverVersion(
    UUID id, 
    UUID manuscriptId, 
    String fileUrl, 
    UUID uploadedByUserId, 
    int versionNumber, 
    Instant uploadedAt
) {}
