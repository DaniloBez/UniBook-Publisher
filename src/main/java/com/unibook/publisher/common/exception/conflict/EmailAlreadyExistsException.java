package com.unibook.publisher.common.exception.conflict;

public class EmailAlreadyExistsException extends DuplicateResourceException {
    public EmailAlreadyExistsException(String email) {
        super(String.format("Користувач з поштою '%s' вже існує", email));
    }
}
