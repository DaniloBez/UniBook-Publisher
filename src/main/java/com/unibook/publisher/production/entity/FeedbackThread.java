package com.unibook.publisher.production.entity;

import java.time.Instant;
import java.util.UUID;

import com.unibook.publisher.production.enums.SuggestionStatus;
import com.unibook.publisher.production.enums.ThreadStatus;

public record FeedbackThread(
    UUID id,
    UUID chapterId,
    UUID createdByUserId,
    ThreadStatus status,
    boolean isSuggestion,
    String suggestedText,
    SuggestionStatus suggestionStatus,
    Instant createdAt
) {}
