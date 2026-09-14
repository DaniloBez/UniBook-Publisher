package com.unibook.publisher.common.event;

import com.unibook.publisher.common.enums.ThreadType;

import java.util.UUID;

public record ThreadOpenedEvent(
        UUID manuscriptId,
        String manuscriptTitle,
        ThreadType threadType,
        UUID threadId,
        UUID chapterId,
        UUID initiatorId,
        UUID recipientId,
        String topicTitle
) {
}
