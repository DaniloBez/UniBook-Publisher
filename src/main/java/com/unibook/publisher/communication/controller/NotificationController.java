package com.unibook.publisher.communication.controller;

import com.unibook.publisher.communication.entity.response.NotificationResponse;
import com.unibook.publisher.communication.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {
    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ResponseEntity<List<NotificationResponse>> getNotifications(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestParam(required = false, defaultValue = "false") Boolean unreadOnly
    ) {
        return ResponseEntity.ok(notificationService.getUserNotifications(userId, unreadOnly));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<NotificationResponse> markAsRead(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(notificationService.markAsRead(id, userId));
    }
}
