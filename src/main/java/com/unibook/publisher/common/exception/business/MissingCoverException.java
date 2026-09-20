package com.unibook.publisher.common.exception.business;

public class MissingCoverException extends BusinessRuleViolationException {
    public MissingCoverException() {
        super("Публікація неможлива без жодної версії обкладинки");
    }
}
