package com.unibook.publisher.common.event;

import java.util.UUID;

public record CoverVersionAddedEvent(
        UUID manuscriptId,
        String manuscriptTitle,
        UUID coverVersionId,
        UUID designerId,
        UUID authorId
) {
}
