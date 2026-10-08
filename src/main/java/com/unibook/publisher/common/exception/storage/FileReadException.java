package com.unibook.publisher.common.exception.storage;

public class FileReadException extends RuntimeException {
    public FileReadException(String name) {
        super("Не вдалося прочитати файл " + name);
    }

    public FileReadException(String name, Throwable cause) {
        super("Не вдалося прочитати файл " + name, cause);
    }
}
