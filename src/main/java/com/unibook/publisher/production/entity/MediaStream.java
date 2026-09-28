package com.unibook.publisher.production.entity;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;

public record MediaStream( //Dont turn into @Entity, this is internal DTO
        InputStream stream,
        String contentType,
        long size
) implements Closeable {

    @Override
    public void close() throws IOException {
        stream.close();
    }
}
