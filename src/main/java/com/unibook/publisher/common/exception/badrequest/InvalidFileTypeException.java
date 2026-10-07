package com.unibook.publisher.common.exception.badrequest;

public class InvalidFileTypeException extends BadRequestException {
    public InvalidFileTypeException(String message) {
        super(message);
    }
}
