package com.unibook.publisher.common.exception.business;

import com.unibook.publisher.common.exception.DomainException;

public class BusinessRuleViolationException extends DomainException {
    public BusinessRuleViolationException(String message) {
        super(message);
    }
}
