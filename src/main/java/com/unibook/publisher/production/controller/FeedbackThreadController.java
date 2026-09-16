package com.unibook.publisher.production.controller;

import com.unibook.publisher.production.entity.request.OpenThreadRequest;
import com.unibook.publisher.production.entity.request.ThreadMessageRequest;
import com.unibook.publisher.production.entity.response.ThreadMessageResponse;
import com.unibook.publisher.production.entity.response.ThreadResponse;
import com.unibook.publisher.production.enums.ThreadStatus;
import com.unibook.publisher.production.service.FeedbackThreadService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class FeedbackThreadController {
    private final FeedbackThreadService threadService;

    public FeedbackThreadController(FeedbackThreadService threadService) {
        this.threadService = threadService;
    }

    @PostMapping("/chapters/{chapterId}/threads")
    public ResponseEntity<ThreadResponse> openThread(
            @PathVariable UUID chapterId,
            @RequestHeader("X-User-Id") UUID userId,
            @Valid @RequestBody OpenThreadRequest request
    ) {
        return ResponseEntity.ok(threadService.openThread(chapterId, userId, request));
    }

    @GetMapping("/chapters/{chapterId}/threads")
    public ResponseEntity<List<ThreadResponse>> getThreads(
            @PathVariable UUID chapterId,
            @RequestParam(required = false) ThreadStatus status
    ) {
        return ResponseEntity.ok(threadService.getThreads(chapterId, status));
    }

    @PostMapping("/threads/{id}/messages")
    public ResponseEntity<ThreadMessageResponse> addMessage(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") UUID userId,
            @Valid @RequestBody ThreadMessageRequest request
    ) {
        return ResponseEntity.ok(threadService.addMessage(id, userId, request));
    }

    @PostMapping("/threads/{id}/accept")
    public ResponseEntity<ThreadResponse> acceptSuggestion(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") UUID userId
    ) {
        return ResponseEntity.ok(threadService.acceptSuggestion(id, userId));
    }

    @PostMapping("/threads/{id}/reject")
    public ResponseEntity<ThreadResponse> rejectSuggestion(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") UUID userId
    ) {
        return ResponseEntity.ok(threadService.rejectSuggestion(id, userId));
    }

    @PatchMapping("/threads/{id}/resolve")
    public ResponseEntity<ThreadResponse> resolveThread(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") UUID userId
    ) {
        return ResponseEntity.ok(threadService.resolveThread(id, userId));
    }
}
