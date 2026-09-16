package com.unibook.publisher.production.entity.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.unibook.publisher.production.entity.FeedbackThread;
import com.unibook.publisher.production.enums.SuggestionStatus;
import com.unibook.publisher.production.enums.ThreadStatus;

import java.time.Instant;
import java.util.UUID;

public record ThreadResponse(
        UUID id,

        @JsonProperty("chapter_id")
        UUID chapterId,

        @JsonProperty("created_by_user_id")
        UUID createdByUserId,

        ThreadStatus status,

        @JsonProperty("is_suggestion")
        boolean isSuggestion,

        @JsonProperty("suggested_text")
        String suggestedText,

        @JsonProperty("suggestion_status")
        SuggestionStatus suggestionStatus,

        @JsonProperty("created_at")
        Instant createdAt
) {
    public static ThreadResponse from(FeedbackThread thread) {
        return new ThreadResponse(
                thread.id(),
                thread.chapterId(),
                thread.createdByUserId(),
                thread.status(),
                thread.isSuggestion(),
                thread.suggestedText(),
                thread.suggestionStatus(),
                thread.createdAt()
        );
    }
}
