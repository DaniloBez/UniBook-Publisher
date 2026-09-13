package com.unibook.publisher.common.event;

import java.util.UUID;

public record ManuscriptSubmittedEvent(UUID manuscriptId, UUID authorId) {
}
