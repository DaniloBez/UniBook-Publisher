package com.unibook.publisher.production.enums;

import java.util.Set;

public enum ManuscriptStatus {
    SUBMITTED,
    POSTPONED,
    IN_PROGRESS,
    TEXT_APPROVED,
    IN_DESIGN,
    PUBLISHED,
    REJECTED;

    public boolean canTransitionTo(ManuscriptStatus next) {
        return switch (this) {
            case SUBMITTED -> next == POSTPONED || next == REJECTED || next == IN_PROGRESS;
            case POSTPONED -> next == IN_PROGRESS || next == REJECTED;
            case REJECTED -> false;
            case IN_PROGRESS -> next == TEXT_APPROVED;
            case TEXT_APPROVED -> next == IN_DESIGN;
            case IN_DESIGN -> next == PUBLISHED;
            case PUBLISHED -> false;
        };
    }

    public Set<ManuscriptStatus> allowedTransitions() {
        return switch (this) {
            case SUBMITTED -> Set.of(POSTPONED, REJECTED, IN_PROGRESS);
            case POSTPONED -> Set.of(REJECTED, IN_PROGRESS);
            case REJECTED -> Set.of();
            case IN_PROGRESS -> Set.of(TEXT_APPROVED);
            case TEXT_APPROVED -> Set.of(IN_DESIGN);
            case IN_DESIGN -> Set.of(PUBLISHED);
            case PUBLISHED -> Set.of();
        };
    }
}
