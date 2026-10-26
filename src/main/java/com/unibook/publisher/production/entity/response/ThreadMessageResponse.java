package com.unibook.publisher.production.entity.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.unibook.publisher.identity.api.UserSummary;
import com.unibook.publisher.production.entity.ThreadMessage;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record ThreadMessageResponse(
        UUID id,

        @JsonProperty("thread_id")
        UUID threadId,

        @JsonProperty("sender_user_id")
        UUID senderUserId,

        @JsonProperty("sender_display_name")
        String senderDisplayName,

        @JsonProperty("sender_role")
        String senderRole,

        @JsonProperty("sender_avatar_url")
        String senderAvatarUrl,

        String content,

        @JsonProperty("sent_at")
        Instant sentAt
) {
    public static ThreadMessageResponse from(ThreadMessage message, Map<UUID, UserSummary> userMap) {
        UserSummary sender = userMap != null ? userMap.get(message.getSenderUserId()) : null;

        return new ThreadMessageResponse(
                message.getId(),
                message.getThreadId(),
                message.getSenderUserId(),
                sender != null ? sender.displayName() : null,
                sender != null && sender.role() != null ? sender.role().name() : null,
                sender != null ? sender.avatarUrl() : null,
                message.getContent(),
                message.getSentAt()
        );
    }
}
