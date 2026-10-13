package com.unibook.publisher.common.exception.state;

import com.unibook.publisher.common.exception.DomainException;
import lombok.Getter;
import lombok.NonNull;

import java.io.Serializable;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;

@Getter
public class InvalidStateTransitionException extends DomainException {
    private final String entityName;
    private final UUID entityId;
    private final Serializable currentStatus;
    private final Serializable targetStatus;
    private final Set<Serializable> allowedTransitions;

    public <E extends Enum<E>> InvalidStateTransitionException(
            @NonNull String entityName,
            @NonNull UUID entityId,
            @NonNull E currentStatus,
            @NonNull E targetStatus,
            Set<E> allowedTransitions
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
        this.allowedTransitions = allowedTransitions != null
                ? Collections.unmodifiableSet(allowedTransitions)
                : Collections.emptySet();
    }
}
