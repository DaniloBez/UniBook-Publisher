package com.unibook.publisher.common.event;

import java.util.UUID;

public record TextFinalizedEvent(
        UUID manuscriptId,
        String manuscriptTitle,
        UUID editorId,
        UUID authorId
) {
}
