package com.unibook.publisher.common.exception.business;

public class ThreadNotASuggestionException extends BusinessRuleViolationException {
    public ThreadNotASuggestionException() {
        super("Тред не містить пропозиції заміни тексту");
    }
}
