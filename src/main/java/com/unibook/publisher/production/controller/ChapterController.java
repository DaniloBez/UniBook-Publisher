package com.unibook.publisher.production.controller;

import com.unibook.publisher.production.entity.request.ChapterCreationRequest;
import com.unibook.publisher.production.entity.request.RevisionUploadRequest;
import com.unibook.publisher.production.entity.response.ChapterResponse;
import com.unibook.publisher.production.entity.response.RevisionResponse;
import com.unibook.publisher.production.service.ChapterService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class ChapterController {
    private final ChapterService chapterService;

    public ChapterController(ChapterService chapterService) {
        this.chapterService = chapterService;
    }

    @PostMapping("/manuscripts/{manuscriptId}/chapters")
    public ResponseEntity<ChapterResponse> createChapter(
            @PathVariable UUID manuscriptId,
            @RequestHeader("X-User-Id") UUID authorId,
            @Valid @RequestBody ChapterCreationRequest request
    ) {
        ChapterResponse response = chapterService.createChapter(manuscriptId, authorId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/manuscripts/{manuscriptId}/chapters")
    public ResponseEntity<List<ChapterResponse>> getChaptersByManuscriptId(
            @PathVariable UUID manuscriptId
    ) {
        return  ResponseEntity.ok(chapterService.getChaptersByManuscriptId(manuscriptId));
    }

    @PostMapping("/chapters/{chapterId}/revisions")
    public ResponseEntity<RevisionResponse> uploadRevision(
            @PathVariable UUID chapterId,
            @RequestHeader("X-User-Id") UUID userId,
            @Valid @RequestBody RevisionUploadRequest request
    ) {
        RevisionResponse response = chapterService.uploadRevision(chapterId, userId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/chapters/{chapterId}/revisions")
    public ResponseEntity<List<RevisionResponse>> getRevisionsByChapterId(
            @PathVariable UUID chapterId
    ) {
        return ResponseEntity.ok(chapterService.getRevisionsByChapterId(chapterId));
    }
}
