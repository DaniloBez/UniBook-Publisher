package com.unibook.publisher.common.exception.business;

import com.unibook.publisher.common.exception.DomainException;

public class FileUploadException extends DomainException {
    public FileUploadException(String message, Throwable cause) {
        super(message, cause);
    }
}
