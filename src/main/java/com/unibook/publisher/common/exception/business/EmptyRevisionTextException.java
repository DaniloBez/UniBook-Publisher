package com.unibook.publisher.common.exception.business;

public class EmptyRevisionTextException extends BusinessRuleViolationException {
    public EmptyRevisionTextException() {
        super("Текст ревізії порожній");
    }
}
