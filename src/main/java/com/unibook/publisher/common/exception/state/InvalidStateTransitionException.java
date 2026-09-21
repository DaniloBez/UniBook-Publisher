package com.unibook.publisher.common.exception.state;

import com.unibook.publisher.common.exception.DomainException;

import java.util.Collections;
import java.util.Set;

public class InvalidStateTransitionException extends DomainException {
    private final String entityName;
    private final Object entityId;
    private final Object currentStatus;
    private final Object targetStatus;
    private final Set<?> allowedTransitions;

    public InvalidStateTransitionException(
            String entityName,
            Object entityId,
            Object currentStatus,
            Object targetStatus,
            Set<?> allowedTransitions
    ) {
        super(String.format(
                "Неможливо перевести %s [ID: %s] зі статусу %s у статус %s. Дозволені переходи: %s",
                entityName,
                entityId,
                currentStatus,
                targetStatus,
                allowedTransitions != null ? allowedTransitions : Collections.emptySet()
        ));
        this.entityName = entityName;
        this.entityId = entityId;
        this.currentStatus = currentStatus;
        this.targetStatus = targetStatus;
        this.allowedTransitions = allowedTransitions != null ? allowedTransitions : Collections.emptySet();
    }

    public String getEntityName() {
        return entityName;
    }

    public Object getEntityId() {
        return entityId;
    }

    public Object getCurrentStatus() {
        return currentStatus;
    }

    public Object getTargetStatus() {
        return targetStatus;
    }

    public Set<?> getAllowedTransitions() {
        return allowedTransitions;
    }
}
