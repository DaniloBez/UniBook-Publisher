package com.unibook.publisher.common.exception.security;

import com.unibook.publisher.common.exception.DomainException;

public class InvalidCredentialsException extends DomainException {
    public InvalidCredentialsException() {
        super("Неправильна пошта або пароль");
    }
}
