package com.unibook.publisher.production.entity;

import java.util.UUID;

public record Chapter(
    UUID chapterId,
    UUID manuscriptId,
    String chapterTitle,
    int chapterIndex //порядковий номер розділу
) {}
