package com.unibook.publisher.common.exception.notfound;

import java.util.UUID;

public class ManuscriptNotFoundException extends ResourceNotFoundException {
    public ManuscriptNotFoundException(UUID id) {
        super("Рукопис", id);
    }
}
