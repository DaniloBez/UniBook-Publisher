package com.unibook.publisher.production.entity.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.unibook.publisher.identity.api.UserSummary;
import com.unibook.publisher.production.entity.Genre;
import com.unibook.publisher.production.enums.ManuscriptStatus;
import com.unibook.publisher.production.entity.Manuscript;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record ManuscriptResponse (
        @JsonProperty("manuscript_id")
        UUID manuscriptId,

        String title,

        @JsonProperty("author_id")
        UUID authorId,

        @JsonProperty("author_display_name")
        String authorDisplayName,

        ManuscriptStatus status,

        List<GenreResponse> genres,

        String annotation,

        @JsonProperty("draft_file_url")
        String draftFileUrl,

        @JsonProperty("submitted_at")
        Instant submittedAt
) {
    public static ManuscriptResponse from(Manuscript manuscript, Map<UUID, UserSummary> userMap) {
        UserSummary author = userMap != null ? userMap.get(manuscript.getAuthorId()) : null;

        List<GenreResponse> genreResponses = (manuscript.getGenres() != null)
                ? manuscript.getGenres().stream()
                .map(GenreResponse::from)
                .toList() : Collections.emptyList();

        return new ManuscriptResponse(
                manuscript.getManuscriptId(),
                manuscript.getTitle(),
                manuscript.getAuthorId(),
                author != null ? author.displayName() : null,
                manuscript.getStatus(),
                genreResponses,
                manuscript.getAnnotation(),
                manuscript.getDraftFileUrl(),
                manuscript.getSubmittedAt()
        );
    }
}