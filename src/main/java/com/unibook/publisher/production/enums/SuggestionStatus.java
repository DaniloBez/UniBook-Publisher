package com.unibook.publisher.production.enums;

import java.util.Set;

public enum SuggestionStatus {
    PENDING, 
    ACCEPTED, 
    REJECTED;

    public boolean canTransitionTo(SuggestionStatus next) {
        return switch (this) {
            case PENDING -> next == ACCEPTED || next == REJECTED;
            case ACCEPTED, REJECTED -> false;
        };
    }

    public Set<SuggestionStatus> allowedTransitions() {
        return switch (this) {
            case PENDING -> Set.of(ACCEPTED, REJECTED);
            case ACCEPTED, REJECTED -> Set.of();
        };
    }
}
