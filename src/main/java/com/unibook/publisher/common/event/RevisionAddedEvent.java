package com.unibook.publisher.common.event;

import java.util.UUID;

public record RevisionAddedEvent(UUID manuscriptId, UUID chapterId, UUID revisionId) {
}
