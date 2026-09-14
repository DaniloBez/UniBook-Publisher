package com.unibook.publisher.common.event;

import java.util.UUID;

public record RevisionAddedEvent(
        UUID manuscriptId,
        String manuscriptTitle,
        UUID chapterId,
        UUID revisionId,
        UUID uploaderId,
        UUID recipientId
) {
}
