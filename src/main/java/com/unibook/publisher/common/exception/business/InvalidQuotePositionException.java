package com.unibook.publisher.common.exception.business;

public class InvalidQuotePositionException extends BusinessRuleViolationException {
    public InvalidQuotePositionException() {
        super("Некоректні межі позицій цитати");
    }
}
