package com.unibook.publisher.common.exception.notfound;

import com.unibook.publisher.common.exception.DomainException;

public class ResourceNotFoundException extends DomainException {
    public ResourceNotFoundException(String resourceName, Object id) {
        super(String.format("%s за ID: %s не знайдено", resourceName, id));
    }
}
