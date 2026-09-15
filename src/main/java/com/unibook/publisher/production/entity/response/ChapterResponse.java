package com.unibook.publisher.production.entity.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.unibook.publisher.production.entity.Chapter;

import java.util.UUID;

public record ChapterResponse(
        @JsonProperty("chapter_id")
        UUID chapterId,

        @JsonProperty("manuscript_id")
        UUID manuscriptId,

        @JsonProperty("chapter_title")
        String chapterTitle,

        @JsonProperty("chapter_index")
        int chapterIndex
)
{
    public static ChapterResponse from(Chapter chapter) {
        return new ChapterResponse(
                chapter.chapterId(),
                chapter.manuscriptId(),
                chapter.chapterTitle(),
                chapter.chapterIndex()
        );
    }
}
