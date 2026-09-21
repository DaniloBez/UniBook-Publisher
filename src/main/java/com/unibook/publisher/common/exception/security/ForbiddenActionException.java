package com.unibook.publisher.common.exception.security;

import com.unibook.publisher.common.exception.DomainException;

public class ForbiddenActionException extends DomainException {
    public ForbiddenActionException(String message) {
        super(message);
    }
}
