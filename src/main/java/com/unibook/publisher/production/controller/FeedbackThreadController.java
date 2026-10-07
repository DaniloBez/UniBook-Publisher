package com.unibook.publisher.production.controller;

import com.unibook.publisher.production.entity.request.OpenThreadRequest;
import com.unibook.publisher.production.entity.request.ThreadMessageRequest;
import com.unibook.publisher.production.entity.response.ThreadMessageResponse;
import com.unibook.publisher.production.entity.response.ThreadResponse;
import com.unibook.publisher.production.enums.ThreadStatus;
import com.unibook.publisher.production.service.FeedbackThreadService;
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

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Feedback threads", description = "Discussion threads on chapter text between the author and the editor, including text change suggestions")
public class FeedbackThreadController {
    private final FeedbackThreadService threadService;

    public FeedbackThreadController(FeedbackThreadService threadService) {
        this.threadService = threadService;
    }

    @PostMapping("/chapters/{chapterId}/threads")
    @Operation(summary = "Open a feedback thread", description = "Opens a thread on a chapter with an initial message. If suggestedText is provided, the thread becomes a text replacement suggestion with PENDING status. An optional quote is validated against the target revision text")
        @ApiResponse(responseCode = "200", description = "Thread successfully opened")
        @ApiResponse(responseCode = "400", description = "Request validation error", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
        @ApiResponse(responseCode = "404", description = "Chapter or target revision not found", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
        @ApiResponse(responseCode = "422", description = "The quote is invalid: empty revision text, wrong positions, or the quoted text does not match the revision", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<ThreadResponse> openThread(
            @Parameter(description = "Chapter ID", required = true, example = "9b2d5c1e-3f4a-4b8e-a1c7-5d6e7f8a9b0c")
            @PathVariable UUID chapterId,
            @Parameter(description = "ID of the user making the request (temporary replacement for JWT)", required = true, example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
            @RequestHeader("X-User-Id") UUID userId,
            @Valid @RequestBody OpenThreadRequest request
    ) {
        return ResponseEntity.ok(threadService.openThread(chapterId, userId, request));
    }

    @GetMapping("/chapters/{chapterId}/threads")
    @Operation(summary = "Get chapter threads", description = "Returns threads of the chapter with their messages, optionally filtered by status")
        @ApiResponse(responseCode = "200", description = "Threads successfully retrieved")
        @ApiResponse(responseCode = "404", description = "Chapter not found", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<List<ThreadResponse>> getThreads(
            @Parameter(description = "Chapter ID", required = true, example = "9b2d5c1e-3f4a-4b8e-a1c7-5d6e7f8a9b0c")
            @PathVariable UUID chapterId,
            @Parameter(description = "Filter: return only threads with this status", example = "OPEN")
            @RequestParam(required = false) ThreadStatus status
    ) {
        return ResponseEntity.ok(threadService.getThreads(chapterId, status));
    }

    @PostMapping("/threads/{id}/messages")
    @Operation(summary = "Add a message to a thread", description = "Posts a reply in the thread. Only the author of the manuscript or the assigned editor can reply")
        @ApiResponse(responseCode = "200", description = "Message successfully added")
        @ApiResponse(responseCode = "400", description = "Request validation error", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
        @ApiResponse(responseCode = "403", description = "The caller is neither the author nor the assigned editor", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
        @ApiResponse(responseCode = "404", description = "Thread not found", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<ThreadMessageResponse> addMessage(
            @Parameter(description = "Thread ID", required = true, example = "1d4f8a2b-6c3e-4f9a-8b7d-2e5c9a1f0d3b")
            @PathVariable UUID id,
            @Parameter(description = "ID of the user making the request (temporary replacement for JWT)", required = true, example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
            @RequestHeader("X-User-Id") UUID userId,
            @Valid @RequestBody ThreadMessageRequest request
    ) {
        return ResponseEntity.ok(threadService.addMessage(id, userId, request));
    }

    @PostMapping("/threads/{id}/accept")
    @Operation(summary = "Accept a text suggestion", description = "Accepts a PENDING text replacement suggestion. Only the author of the manuscript can do this")
        @ApiResponse(responseCode = "200", description = "Suggestion accepted")
        @ApiResponse(responseCode = "403", description = "Only the author of the manuscript can accept a suggestion", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
        @ApiResponse(responseCode = "404", description = "Thread not found", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
        @ApiResponse(responseCode = "422", description = "The thread is not a suggestion, or the suggestion is no longer PENDING", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<ThreadResponse> acceptSuggestion(
            @Parameter(description = "Thread ID", required = true, example = "1d4f8a2b-6c3e-4f9a-8b7d-2e5c9a1f0d3b")
            @PathVariable UUID id,
            @Parameter(description = "ID of the author making the request (temporary replacement for JWT)", required = true, example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
            @RequestHeader("X-User-Id") UUID userId
    ) {
        return ResponseEntity.ok(threadService.acceptSuggestion(id, userId));
    }

    @PostMapping("/threads/{id}/reject")
    @Operation(summary = "Reject a text suggestion", description = "Rejects a PENDING text replacement suggestion. Only the author of the manuscript can do this")
        @ApiResponse(responseCode = "200", description = "Suggestion rejected")
        @ApiResponse(responseCode = "403", description = "Only the author of the manuscript can reject a suggestion", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
        @ApiResponse(responseCode = "404", description = "Thread not found", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
        @ApiResponse(responseCode = "422", description = "The thread is not a suggestion, or the suggestion is no longer PENDING", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<ThreadResponse> rejectSuggestion(
            @Parameter(description = "Thread ID", required = true, example = "1d4f8a2b-6c3e-4f9a-8b7d-2e5c9a1f0d3b")
            @PathVariable UUID id,
            @Parameter(description = "ID of the author making the request (temporary replacement for JWT)", required = true, example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
            @RequestHeader("X-User-Id") UUID userId
    ) {
        return ResponseEntity.ok(threadService.rejectSuggestion(id, userId));
    }

    @PatchMapping("/threads/{id}/resolve")
    @Operation(summary = "Resolve a thread", description = "Closes the thread by setting its status to RESOLVED. Only the author of the manuscript or the assigned editor can do this")
        @ApiResponse(responseCode = "200", description = "Thread resolved")
        @ApiResponse(responseCode = "403", description = "The caller is neither the author nor the assigned editor", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
        @ApiResponse(responseCode = "404", description = "Thread not found", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
        @ApiResponse(responseCode = "422", description = "The thread is already resolved", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<ThreadResponse> resolveThread(
            @Parameter(description = "Thread ID", required = true, example = "1d4f8a2b-6c3e-4f9a-8b7d-2e5c9a1f0d3b")
            @PathVariable UUID id,
            @Parameter(description = "ID of the user making the request (temporary replacement for JWT)", required = true, example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
            @RequestHeader("X-User-Id") UUID userId
    ) {
        return ResponseEntity.ok(threadService.resolveThread(id, userId));
    }

    @DeleteMapping("/threads/{id}")
    @Operation(summary = "Delete a thread", description = "Deletes the thread with all its messages. Only the author of the manuscript or the assigned editor can do this")
        @ApiResponse(responseCode = "204", description = "Thread successfully deleted")
        @ApiResponse(responseCode = "403", description = "The caller is neither the author nor the assigned editor", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
        @ApiResponse(responseCode = "404", description = "Thread not found", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<Void> deleteThread(
            @Parameter(description = "Thread ID", required = true, example = "1d4f8a2b-6c3e-4f9a-8b7d-2e5c9a1f0d3b")
            @PathVariable UUID id,
            @Parameter(description = "ID of the user making the request (temporary replacement for JWT)", required = true, example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
            @RequestHeader("X-User-Id") UUID userId
    ) {
        threadService.deleteThread(id, userId);
        return ResponseEntity.noContent().build();
    }
}
