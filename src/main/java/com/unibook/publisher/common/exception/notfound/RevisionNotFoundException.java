package com.unibook.publisher.common.exception.notfound;

import java.util.UUID;

public class RevisionNotFoundException extends ResourceNotFoundException {
    public RevisionNotFoundException(UUID id) {
        super("Ревізію", id);
    }
}
