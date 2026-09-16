package com.unibook.publisher.production.entity.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.unibook.publisher.production.entity.CoverVersion;

import java.time.Instant;
import java.util.UUID;

public record CoverVersionResponse(
        UUID id,

        @JsonProperty("manuscript_id")
        UUID manuscriptId,

        @JsonProperty("file_url")
        String fileUrl,

        @JsonProperty("uploaded_by_user_id")
        UUID uploadedByUserId,

        @JsonProperty("version_number")
        int versionNumber,

        @JsonProperty("uploaded_at")
        Instant uploadedAt
) {
    public static CoverVersionResponse from(CoverVersion coverVersion) {
        return new CoverVersionResponse(
                coverVersion.id(),
                coverVersion.manuscriptId(),
                coverVersion.fileUrl(),
                coverVersion.uploadedByUserId(),
                coverVersion.versionNumber(),
                coverVersion.uploadedAt()
        );
    }
}
