package com.unibook.publisher.production.controller;

import com.unibook.publisher.common.exception.storage.FileReadException;
import com.unibook.publisher.production.entity.request.ChapterCreationRequest;
import com.unibook.publisher.production.entity.request.ChapterUpdateRequest;
import com.unibook.publisher.production.entity.request.DiffRequest;
import com.unibook.publisher.production.entity.response.ChapterResponse;
import com.unibook.publisher.production.entity.response.DiffResponse;
import com.unibook.publisher.production.entity.response.RevisionResponse;
import com.unibook.publisher.production.service.ChapterService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
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

    @PutMapping("/chapters/{chapterId}")
    public ResponseEntity<ChapterResponse> updateChapter(
            @PathVariable UUID chapterId,
            @RequestHeader("X-User-Id") UUID authorId,
            @Valid @RequestBody ChapterUpdateRequest request
    ) {
        return ResponseEntity.ok(chapterService.updateChapter(chapterId, authorId, request));
    }

    @DeleteMapping("/chapters/{chapterId}")
    public ResponseEntity<Void> deleteChapter(
            @PathVariable UUID chapterId,
            @RequestHeader("X-User-Id") UUID authorId
    ) {
        chapterService.deleteChapter(chapterId, authorId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/chapters/{chapterId}/revisions")
    public ResponseEntity<RevisionResponse> uploadRevision(
            @PathVariable UUID chapterId,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestParam MultipartFile file
    ) {
        try {
            return ResponseEntity.ok(chapterService.uploadRevision(
                    chapterId,
                    userId,
                    file.getInputStream(),
                    file.getSize(),
                    file.getContentType(),
                    file.getOriginalFilename()
            ));
        } catch (IOException _) {
            System.out.printf("Проблема читання файлу " + file.getOriginalFilename());
            throw new FileReadException(file.getOriginalFilename());
        }
    }

    @GetMapping("/chapters/{chapterId}/revisions")
    public ResponseEntity<List<RevisionResponse>> getRevisionsByChapterId(
            @PathVariable UUID chapterId
    ) {
        return ResponseEntity.ok(chapterService.getRevisionsByChapterId(chapterId));
    }

    @GetMapping("/{chapterId}/diff")
    public ResponseEntity<DiffResponse> getDiffChapter(
            @PathVariable UUID chapterId,
            @Valid @ModelAttribute DiffRequest request
    ) {
        DiffResponse response = chapterService.getDiffChapter(chapterId, request);
        return ResponseEntity.ok(response);
    }
}
