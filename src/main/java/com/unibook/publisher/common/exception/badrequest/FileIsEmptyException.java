package com.unibook.publisher.common.exception.badrequest;

public class FileIsEmptyException extends BadRequestException {
    public FileIsEmptyException() {
        super("Файл порожній або не вибраний.");
    }
}
