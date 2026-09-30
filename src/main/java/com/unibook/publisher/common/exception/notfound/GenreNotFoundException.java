package com.unibook.publisher.common.exception.notfound;

import java.util.UUID;

public class GenreNotFoundException extends ResourceNotFoundException {
    public GenreNotFoundException(UUID id) {
        super("Жанр", id);
    }

    public GenreNotFoundException(String name) {
        super(String.format("Жанр з назвою '%s' не знайдено", name));
    }
}
