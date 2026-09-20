package com.unibook.publisher.common.enums;

import java.util.Set;

public enum ContractStatus {
    DRAFT,
    ACTIVE,
    TERMINATED;

    public boolean canTransitionTo(ContractStatus next) {
        return switch (this) {
            case DRAFT -> next == ACTIVE || next == TERMINATED;
            case ACTIVE -> next == TERMINATED;
            case TERMINATED -> false;
        };
    }

    public Set<ContractStatus> allowedTransitions() {
        return switch (this) {
            case DRAFT -> Set.of(ACTIVE, TERMINATED);
            case ACTIVE -> Set.of(TERMINATED);
            case TERMINATED -> Set.of();
        };
    }
}
