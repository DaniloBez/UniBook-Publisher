package com.unibook.publisher.common.exception.storage;

public class FileStorageException extends RuntimeException {
    public FileStorageException(String url) {
        super("Помилка при спробі доступу до сховища для файлу/директорії " + url);
    }
}
