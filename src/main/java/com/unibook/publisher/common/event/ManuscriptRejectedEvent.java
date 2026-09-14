package com.unibook.publisher.common.event;

import java.util.UUID;

public record ManuscriptRejectedEvent(UUID manuscriptId, UUID authorId, String reason) {
}
