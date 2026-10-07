package com.unibook.publisher.common.exception.badrequest;

import com.unibook.publisher.common.exception.DomainException;

public abstract class BadRequestException extends DomainException {
    public BadRequestException(String message) {
        super(message);
    }
}
