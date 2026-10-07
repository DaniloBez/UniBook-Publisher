package com.unibook.publisher.communication.controller;

import com.unibook.publisher.communication.entity.response.NotificationResponse;
import com.unibook.publisher.communication.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@Tag(name = "Notifications", description = "User notifications about events in the publishing workflow")
public class NotificationController {
    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    @Operation(summary = "Get user notifications", description = "Returns notifications of the current user, newest first. Can be limited to unread notifications only")
    @ApiResponse(responseCode = "200", description = "Notifications successfully retrieved")
    public ResponseEntity<List<NotificationResponse>> getNotifications(
            @Parameter(description = "ID of the user making the request (temporary replacement for JWT)", required = true, example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
            @RequestHeader("X-User-Id") UUID userId,
            @Parameter(description = "If true, only unread notifications are returned", example = "false")
            @RequestParam(required = false, defaultValue = "false") Boolean unreadOnly
    ) {
        return ResponseEntity.ok(notificationService.getUserNotifications(userId, unreadOnly));
    }

    @PatchMapping("/{id}/read")
    @Operation(summary = "Mark notification as read", description = "Marks the notification as read. Only the recipient of the notification can do this")
        @ApiResponse(responseCode = "200", description = "Notification marked as read")
        @ApiResponse(responseCode = "403", description = "The notification belongs to another user", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
        @ApiResponse(responseCode = "404", description = "Notification not found", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<NotificationResponse> markAsRead(
            @Parameter(description = "ID of the user making the request (temporary replacement for JWT)", required = true, example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
            @RequestHeader("X-User-Id") UUID userId,
            @Parameter(description = "Notification ID", required = true, example = "7c9e6679-7425-40de-944b-e07fc1f90ae7")
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(notificationService.markAsRead(id, userId));
    }
}
