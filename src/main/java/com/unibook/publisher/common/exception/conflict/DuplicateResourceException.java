package com.unibook.publisher.common.exception.conflict;

import com.unibook.publisher.common.exception.DomainException;

public class DuplicateResourceException extends DomainException {
    public DuplicateResourceException(String message) {
        super(message);
    }
}
