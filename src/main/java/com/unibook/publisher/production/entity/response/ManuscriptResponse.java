package com.unibook.publisher.production.entity.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.unibook.publisher.production.entity.Genre;
import com.unibook.publisher.production.enums.ManuscriptStatus;
import com.unibook.publisher.production.entity.Manuscript;

import java.time.Instant;
import java.util.Collections;
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
        List<UUID> genreIds = (manuscript.getGenres() != null)
                ? manuscript.getGenres().stream()
                .map(Genre::getGenreId)
                .toList() : Collections.emptyList();

        return new ManuscriptResponse(
                manuscript.getManuscriptId(),
                manuscript.getTitle(),
                manuscript.getAuthorId(),
                manuscript.getStatus(),
                genreIds,
                manuscript.getAnnotation(),
                manuscript.getDraftFileUrl(),
                manuscript.getSubmittedAt()
        );
    }
}