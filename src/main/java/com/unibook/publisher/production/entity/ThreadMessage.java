package com.unibook.publisher.production.entity;

import java.time.Instant;
import java.util.UUID;

public record ThreadMessage(
    UUID id, 
    UUID threadId, 
    UUID senderUserId, 
    String content, 
    Instant sentAt
) {}
