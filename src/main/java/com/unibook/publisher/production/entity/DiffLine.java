package com.unibook.publisher.production.entity;

import com.unibook.publisher.production.enums.DiffStatus;

public record DiffLine(
        String content,
        DiffStatus status,
        Integer oldLineIndex,
        Integer newLineIndex
) {}
