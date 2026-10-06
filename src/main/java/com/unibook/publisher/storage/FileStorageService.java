package com.unibook.publisher.storage;

import java.io.InputStream;

public interface FileStorageService {

    InputStream get(String fileUrl);

    String getPresignedUrl(String path);

    String put(String path, InputStream content, long size, String contentType);

    boolean exists(String fileUrl);

    void delete(String fileUrl);
}
