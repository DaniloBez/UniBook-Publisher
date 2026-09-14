package com.unibook.publisher.common.event;

import java.util.UUID;

public record ManuscriptApprovedEvent(UUID manuscriptId, UUID editorId, UUID authorId) {
}
