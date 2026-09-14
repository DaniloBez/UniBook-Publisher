package com.unibook.publisher.common.event;

import java.util.UUID;

public record ThreadMessageAddedEvent(UUID manuscriptId, UUID threadId, UUID recipientUserId, UUID senderId, String message) {
}
