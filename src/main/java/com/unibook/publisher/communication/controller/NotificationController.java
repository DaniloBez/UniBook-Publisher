package com.unibook.publisher.communication.controller;

import com.unibook.publisher.communication.entity.response.NotificationResponse;
import com.unibook.publisher.communication.entity.response.UnreadCountResponse;
import com.unibook.publisher.communication.service.NotificationService;
import com.unibook.publisher.common.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @Parameter(description = "If true, only unread notifications are returned", example = "false")
            @RequestParam(required = false, defaultValue = "false") Boolean unreadOnly
    ) {
        return ResponseEntity.ok(notificationService.getUserNotifications(currentUser, unreadOnly));
    }

    @PatchMapping("/{id}/read")
    @Operation(summary = "Mark notification as read", description = "Marks the notification as read. Only the recipient of the notification can do this")
    @ApiResponse(responseCode = "200", description = "Notification marked as read")
    @ApiResponse(responseCode = "403", description = "The notification belongs to another user", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "Notification not found", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<NotificationResponse> markAsRead(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @Parameter(description = "Notification ID", required = true, example = "7c9e6679-7425-40de-944b-e07fc1f90ae7")
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(notificationService.markAsRead(id, currentUser));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Get unread notifications count", description = "Returns the number of unread notifications for the current user")
    @ApiResponse(responseCode = "200", description = "Unread count successfully retrieved")
    public ResponseEntity<UnreadCountResponse> getUnreadCount(
            @AuthenticationPrincipal AuthenticatedUser currentUser
    ) {
        return ResponseEntity.ok(notificationService.getUnreadCount(currentUser.id()));
    }

    @PatchMapping("/read-all")
    @Operation(summary = "Mark all notifications as read", description = "Marks every notification belonging to the current user as read")
    @ApiResponse(responseCode = "204", description = "All notifications successfully marked as read")
    public ResponseEntity<Void> markAllAsRead(
            @AuthenticationPrincipal AuthenticatedUser currentUser
    ) {
        notificationService.markAllAsRead(currentUser.id());
        return ResponseEntity.noContent().build();
    }
}
