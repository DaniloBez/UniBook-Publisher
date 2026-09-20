package com.unibook.publisher.common.exception.notfound;

import java.util.UUID;

public class ThreadNotFoundException extends ResourceNotFoundException {
    public ThreadNotFoundException(UUID id) {
        super("Тред", id);
    }
}
