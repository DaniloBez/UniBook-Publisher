package com.unibook.publisher.production.enums;

import java.util.Set;

public enum ThreadStatus {
    OPEN,
    RESOLVED;

    public boolean canTransitionTo(ThreadStatus next) {
        return switch (this) {
            case OPEN -> next == RESOLVED;
            case RESOLVED -> next == OPEN;
        };
    }

    public Set<ThreadStatus> allowedTransitions() {
        return switch (this) {
            case OPEN -> Set.of(RESOLVED);
            case RESOLVED -> Set.of(OPEN);
        };
    }
}
