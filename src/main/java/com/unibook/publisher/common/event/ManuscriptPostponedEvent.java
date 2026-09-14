package com.unibook.publisher.common.event;

import java.util.UUID;

public record ManuscriptPostponedEvent(UUID manuscriptId, UUID authorId, UUID comment) {
}
