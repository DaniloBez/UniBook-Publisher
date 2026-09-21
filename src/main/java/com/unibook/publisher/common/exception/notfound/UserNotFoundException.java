package com.unibook.publisher.common.exception.notfound;

import java.util.UUID;

public class UserNotFoundException extends ResourceNotFoundException {
    public UserNotFoundException(UUID id) {
        super("Користувача", id);
    }
}
