package com.unibook.publisher.common.event;

import java.util.UUID;

public record ThreadMessageAddedEvent(
        UUID manuscriptId,
        UUID threadId,
        String threadTitle,
        UUID senderId,
        UUID recipientUserId,
        String message
) {
}
