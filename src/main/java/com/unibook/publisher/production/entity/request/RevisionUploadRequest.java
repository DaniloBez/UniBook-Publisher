package com.unibook.publisher.production.entity.request;

import jakarta.validation.constraints.NotBlank;

public record RevisionUploadRequest(
        @NotBlank
        String fileUrl
) {}
