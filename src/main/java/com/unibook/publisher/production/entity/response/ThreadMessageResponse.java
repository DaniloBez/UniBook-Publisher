package com.unibook.publisher.production.entity.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.unibook.publisher.production.entity.ThreadMessage;

import java.time.Instant;
import java.util.UUID;

public record ThreadMessageResponse(
        UUID id,

        @JsonProperty("thread_id")
        UUID threadId,

        @JsonProperty("sender_user_id")
        UUID senderUserId,

        String content,

        @JsonProperty("sent_at")
        Instant sentAt
) {
    public static ThreadMessageResponse from(ThreadMessage message) {
        return new ThreadMessageResponse(
                message.id(),
                message.threadId(),
                message.senderUserId(),
                message.content(),
                message.sentAt()
        );
    }
}
