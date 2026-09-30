package com.unibook.publisher.production.api;

import java.util.UUID;

public record ManuscriptDto(
        UUID manuscriptId,
        String title
) {}