package com.unibook.publisher.production.entity.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record DiffRequest(
        @NotNull
        UUID fromRevisionId,

        @NotNull
        UUID toRevisionId
) {}
