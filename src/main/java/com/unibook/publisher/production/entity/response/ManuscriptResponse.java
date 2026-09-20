package com.unibook.publisher.production.entity.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.unibook.publisher.production.enums.ManuscriptStatus;
import com.unibook.publisher.production.entity.Manuscript;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ManuscriptResponse (
        @JsonProperty("manuscript_id")
        UUID manuscriptId,

        String title,

        @JsonProperty("author_id")
        UUID authorId,

        ManuscriptStatus status,

        @JsonProperty("genre_ids")
        List<UUID> genreIds,

        String annotation,

        @JsonProperty("draft_file_url")
        String draftFileUrl,

        @JsonProperty("submitted_at")
        Instant submittedAt
) {
    public static ManuscriptResponse from(Manuscript manuscript) {
        return new ManuscriptResponse(
                manuscript.manuscriptId(),
                manuscript.title(),
                manuscript.authorId(),
                manuscript.status(),
                manuscript.genreIds(),
                manuscript.annotation(),
                manuscript.draftFileUrl(),
                manuscript.submittedAt()
        );
    }
}