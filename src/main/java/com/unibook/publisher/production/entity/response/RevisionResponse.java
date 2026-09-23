package com.unibook.publisher.production.entity.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.unibook.publisher.production.entity.Revision;

import java.time.Instant;
import java.util.UUID;

public record RevisionResponse(
        @JsonProperty("revision_id")
        UUID revisionId,

        @JsonProperty("chapter_id")
        UUID chapterId,

        @JsonProperty("version_number")
        int versionNumber,

        @JsonProperty("file_url")
        String fileUrl,

        @JsonProperty("uploaded_by_user_id")
        UUID uploadedByUserId,

        @JsonProperty("uploaded_at")
        Instant uploadedAt,

        @JsonProperty("text_content")
        String textContent
) {
    public static RevisionResponse from(Revision revision) {
        return new RevisionResponse(
                revision.revisionId(),
                revision.chapterId(),
                revision.versionNumber(),
                revision.fileUrl(),
                revision.uploadedByUserId(),
                revision.uploadedAt(),
                revision.textContent()
        );
    }
}
