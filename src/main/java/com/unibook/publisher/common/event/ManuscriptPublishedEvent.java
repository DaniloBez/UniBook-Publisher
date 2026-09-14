package com.unibook.publisher.common.event;

import java.util.UUID;

public record ManuscriptPublishedEvent(
        UUID manuscriptId,
        String manuscriptTitle,
        UUID authorId
) {
}
