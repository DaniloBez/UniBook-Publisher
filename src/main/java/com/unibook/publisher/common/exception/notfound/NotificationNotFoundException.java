package com.unibook.publisher.common.exception.notfound;

import java.util.UUID;

public class NotificationNotFoundException extends ResourceNotFoundException {
    public NotificationNotFoundException(UUID id) {
        super("Сповіщення", id);
    }
}
