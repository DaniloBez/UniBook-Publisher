package com.unibook.publisher.common.exception.notfound;

import java.util.UUID;

public class ChapterNotFoundException extends ResourceNotFoundException {
    public ChapterNotFoundException(UUID id) {
        super("Розділ", id);
    }
}
