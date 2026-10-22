package com.unibook.publisher.production.entity.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.unibook.publisher.identity.api.UserSummary;
import com.unibook.publisher.production.entity.FeedbackThread;
import com.unibook.publisher.production.enums.SuggestionStatus;
import com.unibook.publisher.production.enums.ThreadStatus;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record ThreadResponse(
        UUID id,

        @JsonProperty("chapter_id")
        UUID chapterId,

        @JsonProperty("created_by_user_id")
        UUID createdByUserId,

        @JsonProperty("target_revision_id")
        UUID targetRevisionId,

        @JsonProperty("quoted_text")
        String quotedText,

        @JsonProperty("position_from")
        Integer positionFrom,

        @JsonProperty("position_to")
        Integer positionTo,

        ThreadStatus status,

        @JsonProperty("is_suggestion")
        boolean isSuggestion,

        @JsonProperty("suggested_text")
        String suggestedText,

        @JsonProperty("suggestion_status")
        SuggestionStatus suggestionStatus,

        @JsonProperty("created_at")
        Instant createdAt,

        List<ThreadMessageResponse> messages
) {
    public static ThreadResponse from(FeedbackThread thread, Map<UUID, UserSummary> userMap) {
        List<ThreadMessageResponse> messages = thread.getMessages() == null ? Collections.emptyList() :
                thread.getMessages().stream()
                        .map(msg -> ThreadMessageResponse.from(msg, userMap))
                        .toList();

        return new ThreadResponse(
                thread.getId(),
                thread.getChapterId(),
                thread.getCreatedByUserId(),
                thread.getTargetRevisionId(),
                thread.getQuotedText(),
                thread.getPositionFrom(),
                thread.getPositionTo(),
                thread.getStatus(),
                thread.isSuggestion(),
                thread.getSuggestedText(),
                thread.getSuggestionStatus(),
                thread.getCreatedAt(),
                messages
        );
    }
}
