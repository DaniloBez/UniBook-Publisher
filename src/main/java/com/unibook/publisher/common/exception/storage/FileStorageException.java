package com.unibook.publisher.common.exception.storage;

import com.unibook.publisher.common.exception.DomainException;

public class FileStorageException extends DomainException {
    public FileStorageException(String url) {
        super("Помилка при спробі доступу до сховища для файлу " + url);
    }

    public FileStorageException(String url, Throwable cause) {
        super("Помилка при спробі доступу до сховища для файлу " + url, cause);
    }
}
