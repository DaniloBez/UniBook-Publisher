package com.unibook.publisher.production.entity;

import com.unibook.publisher.production.enums.ManuscriptStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record Manuscript(
    UUID manuscriptId,
    String title,
    UUID authorId,
    ManuscriptStatus status,
    List<UUID> genreIds,
    String annotation,
    String draftFileUrl, //посилання на файл з чернеткою рукопису
    Instant submittedAt
) {
    public Manuscript withStatus(ManuscriptStatus newStatus) {
        return new Manuscript(manuscriptId, title, authorId, newStatus, genreIds, annotation, draftFileUrl, submittedAt);
    }
}
