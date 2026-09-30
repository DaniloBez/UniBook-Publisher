package com.unibook.publisher.production.api;

import java.util.Optional;
import java.util.UUID;

public interface ManuscriptApi {
    Optional<ManuscriptDto> findById(UUID manuscriptId);
}