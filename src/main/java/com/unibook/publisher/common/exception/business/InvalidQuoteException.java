package com.unibook.publisher.common.exception.business;

public class InvalidQuoteException extends BusinessRuleViolationException {
    public InvalidQuoteException(String quote) {
        super(String.format("Цитований текст '%s' не знайдено в зазначеній версії", quote));
    }
}
