package com.unibook.publisher.common.exception.notfound;

import java.util.UUID;

public class ContractNotFoundException extends ResourceNotFoundException {
    public ContractNotFoundException(UUID id) {
        super("Контракт", id);
    }
}
