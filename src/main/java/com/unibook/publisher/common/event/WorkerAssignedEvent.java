package com.unibook.publisher.common.event;

import com.unibook.publisher.common.enums.UserRole;

import java.util.UUID;

public record WorkerAssignedEvent(UUID manuscriptId, UUID workerId, UserRole workerRole, UUID authorId) {
}
