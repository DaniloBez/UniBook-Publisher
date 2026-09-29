package com.unibook.publisher.common.exception.conflict;

public class GenreAlreadyExistsException extends DuplicateResourceException {
    public GenreAlreadyExistsException(String name) {
        super(String.format("Жанр з назвою '%s' вже існує", name));
    }
}
