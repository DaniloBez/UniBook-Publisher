package com.unibook.publisher.production.entity.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.unibook.publisher.identity.api.UserSummary;
import com.unibook.publisher.production.entity.Revision;

import java.time.Instant;
import java.util.Map;
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

        @JsonProperty("uploaded_by_display_name")
        String uploadedByDisplayName,

        @JsonProperty("uploaded_by_role")
        String uploadedByRole,

        @JsonProperty("uploaded_at")
        Instant uploadedAt
) {
    public static RevisionResponse from(Revision revision, Map<UUID, UserSummary> userMap) {
        UserSummary uploader = userMap != null ? userMap.get(revision.getUploadedByUserId()) : null;

        return new RevisionResponse(
                revision.getRevisionId(),
                revision.getChapterId(),
                revision.getVersionNumber(),
                revision.getFileUrl(),
                revision.getUploadedByUserId(),
                uploader != null ? uploader.displayName() : null,
                uploader != null && uploader.role() != null ? uploader.role().name() : null,
                revision.getUploadedAt()
        );
    }
}
