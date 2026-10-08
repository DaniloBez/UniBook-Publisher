package com.unibook.publisher.production.controller;

import com.unibook.publisher.common.exception.storage.FileReadException;
import com.unibook.publisher.common.logging.AppLogger;
import com.unibook.publisher.production.entity.request.ChapterCreationRequest;
import com.unibook.publisher.production.entity.request.ChapterUpdateRequest;
import com.unibook.publisher.production.entity.request.DiffRequest;
import com.unibook.publisher.production.entity.response.ChapterResponse;
import com.unibook.publisher.production.entity.response.DiffResponse;
import com.unibook.publisher.production.entity.response.RevisionResponse;
import com.unibook.publisher.production.service.ChapterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Chapters", description = "Manuscript chapters and their text revisions")
public class ChapterController {
    private final ChapterService chapterService;
    private final AppLogger logger;

    public ChapterController(ChapterService chapterService, AppLogger logger) {
        this.chapterService = chapterService;
        this.logger = logger;
    }

    @PostMapping("/manuscripts/{manuscriptId}/chapters")
    @Operation(summary = "Create a chapter", description = "Adds a chapter to a manuscript that is IN_PROGRESS. Only the author of the manuscript can do this")
        @ApiResponse(responseCode = "200", description = "Chapter successfully created")
        @ApiResponse(responseCode = "400", description = "Request validation error", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
        @ApiResponse(responseCode = "403", description = "The caller is not the author of the manuscript", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
        @ApiResponse(responseCode = "404", description = "Manuscript not found", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
        @ApiResponse(responseCode = "422", description = "The manuscript is not in IN_PROGRESS status", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<ChapterResponse> createChapter(
            @Parameter(description = "Manuscript ID", required = true, example = "7c9e6679-7425-40de-944b-e07fc1f90ae7")
            @PathVariable UUID manuscriptId,
            @Parameter(description = "ID of the author making the request (temporary replacement for JWT)", required = true, example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
            @RequestHeader("X-User-Id") UUID authorId,
            @Valid @RequestBody ChapterCreationRequest request
    ) {
        ChapterResponse response = chapterService.createChapter(manuscriptId, authorId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/manuscripts/{manuscriptId}/chapters")
    @Operation(summary = "Get chapters of a manuscript", description = "Returns all chapters of the manuscript ordered by chapter index")
        @ApiResponse(responseCode = "200", description = "Chapters successfully retrieved")
        @ApiResponse(responseCode = "404", description = "Manuscript not found", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<List<ChapterResponse>> getChaptersByManuscriptId(
            @Parameter(description = "Manuscript ID", required = true, example = "7c9e6679-7425-40de-944b-e07fc1f90ae7")
            @PathVariable UUID manuscriptId
    ) {
        return  ResponseEntity.ok(chapterService.getChaptersByManuscriptId(manuscriptId));
    }

    @PutMapping("/chapters/{chapterId}")
    @Operation(summary = "Update a chapter", description = "Changes the title and index of a chapter. Only the author of an IN_PROGRESS manuscript can do this")
        @ApiResponse(responseCode = "200", description = "Chapter successfully updated")
        @ApiResponse(responseCode = "400", description = "Request validation error", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
        @ApiResponse(responseCode = "403", description = "The caller is not the author of the manuscript", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
        @ApiResponse(responseCode = "404", description = "Chapter not found", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
        @ApiResponse(responseCode = "422", description = "The manuscript is not in IN_PROGRESS status", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<ChapterResponse> updateChapter(
            @Parameter(description = "Chapter ID", required = true, example = "9b2d5c1e-3f4a-4b8e-a1c7-5d6e7f8a9b0c")
            @PathVariable UUID chapterId,
            @Parameter(description = "ID of the author making the request (temporary replacement for JWT)", required = true, example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
            @RequestHeader("X-User-Id") UUID authorId,
            @Valid @RequestBody ChapterUpdateRequest request
    ) {
        return ResponseEntity.ok(chapterService.updateChapter(chapterId, authorId, request));
    }

    @DeleteMapping("/chapters/{chapterId}")
    @Operation(summary = "Delete a chapter", description = "Deletes a chapter together with its revisions. Only the author of an IN_PROGRESS manuscript can do this")
        @ApiResponse(responseCode = "204", description = "Chapter successfully deleted")
        @ApiResponse(responseCode = "403", description = "The caller is not the author of the manuscript", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
        @ApiResponse(responseCode = "404", description = "Chapter not found", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
        @ApiResponse(responseCode = "422", description = "The manuscript is not in IN_PROGRESS status", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<Void> deleteChapter(
            @Parameter(description = "Chapter ID", required = true, example = "9b2d5c1e-3f4a-4b8e-a1c7-5d6e7f8a9b0c")
            @PathVariable UUID chapterId,
            @Parameter(description = "ID of the author making the request (temporary replacement for JWT)", required = true, example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
            @RequestHeader("X-User-Id") UUID authorId
    ) {
        chapterService.deleteChapter(chapterId, authorId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/chapters/{chapterId}/revisions")
    @Operation(summary = "Upload a chapter revision", description = "Creates a new revision of the chapter from an uploaded file and increments the version number. Available to the author and the assigned editor while the manuscript is IN_PROGRESS")
        @ApiResponse(responseCode = "200", description = "Revision successfully created")
        @ApiResponse(responseCode = "400", description = "Request validation error", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
        @ApiResponse(responseCode = "403", description = "The caller is neither the author nor the assigned editor", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
        @ApiResponse(responseCode = "404", description = "Chapter, manuscript or uploaded file not found", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
        @ApiResponse(responseCode = "422", description = "The manuscript is not in IN_PROGRESS status", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<RevisionResponse> uploadRevision(
            @Parameter(description = "Chapter ID", required = true, example = "9b2d5c1e-3f4a-4b8e-a1c7-5d6e7f8a9b0c")
            @PathVariable UUID chapterId,
            @Parameter(description = "ID of the user making the request (temporary replacement for JWT)", required = true, example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
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
            logger.warn("Проблема читання файлу {}", file.getOriginalFilename());
            throw new FileReadException(file.getOriginalFilename());
        }
    }

    @GetMapping("/chapters/{chapterId}/revisions")
    @Operation(summary = "Get chapter revisions", description = "Returns all revisions of the chapter ordered by version number ascending")
        @ApiResponse(responseCode = "200", description = "Revisions successfully retrieved")
        @ApiResponse(responseCode = "404", description = "Chapter not found", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<List<RevisionResponse>> getRevisionsByChapterId(
            @Parameter(description = "Chapter ID", required = true, example = "9b2d5c1e-3f4a-4b8e-a1c7-5d6e7f8a9b0c")
            @PathVariable UUID chapterId
    ) {
        return ResponseEntity.ok(chapterService.getRevisionsByChapterId(chapterId));
    }

    @GetMapping("/{chapterId}/diff")
    @Operation(summary = "Compare two chapter revisions", description = "Returns the text difference between two revisions of the same chapter")
        @ApiResponse(responseCode = "200", description = "Difference successfully calculated")
        @ApiResponse(responseCode = "400", description = "Missing or invalid query parameters", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
        @ApiResponse(responseCode = "404", description = "Chapter or revision not found", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
        @ApiResponse(responseCode = "422", description = "The requested revisions do not belong to this chapter", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<DiffResponse> getDiffChapter(
            @Parameter(description = "Chapter ID", required = true, example = "9b2d5c1e-3f4a-4b8e-a1c7-5d6e7f8a9b0c")
            @PathVariable UUID chapterId,
            @Valid @ModelAttribute DiffRequest request
    ) {
        DiffResponse response = chapterService.getDiffChapter(chapterId, request);
        return ResponseEntity.ok(response);
    }
}
