package com.unibook.publisher.common.event;

import java.util.UUID;

public record TextFinalizedEvent(UUID manuscriptId, UUID editorId, UUID authorId) {
}
