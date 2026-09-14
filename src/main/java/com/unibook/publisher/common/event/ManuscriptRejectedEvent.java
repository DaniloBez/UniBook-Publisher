package com.unibook.publisher.common.event;

import java.util.UUID;

public record ManuscriptRejectedEvent(
        UUID manuscriptId,
        String manuscriptTitle,
        UUID editorId,
        UUID authorId,
        String reason
) {
}
