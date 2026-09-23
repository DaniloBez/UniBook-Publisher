package com.unibook.publisher.production.entity.response;

import com.unibook.publisher.production.entity.DiffLine;

import java.util.List;
import java.util.UUID;

public record DiffResponse(
    UUID fromRevisionId,
    UUID toRevisionId,
    List<DiffLine> lines
) {}
