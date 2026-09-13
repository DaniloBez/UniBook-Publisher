package com.unibook.publisher.common.event;

import java.util.UUID;

public record ThreadOpenedEvent(UUID manuscriptId, UUID chapterId, UUID threadId, UUID authorId) {
}
