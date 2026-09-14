package com.unibook.publisher.common.event;

import java.util.UUID;

public record ManuscriptPostponedEvent(
        UUID manuscriptId,
        String manuscriptTitle,
        UUID editorId,
        UUID authorId,
        String comment
) {
}
