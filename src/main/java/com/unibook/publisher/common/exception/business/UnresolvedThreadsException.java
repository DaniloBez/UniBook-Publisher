package com.unibook.publisher.common.exception.business;

public class UnresolvedThreadsException extends BusinessRuleViolationException {
    public UnresolvedThreadsException() {
        super("Є незакриті треди або необроблені пропозиції");
    }
}
