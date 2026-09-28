package com.unibook.publisher.common.exception.notfound;

public class FileNotFoundException extends ResourceNotFoundException {
    public FileNotFoundException(String url) {
        super("Файл", url);
    }
}
