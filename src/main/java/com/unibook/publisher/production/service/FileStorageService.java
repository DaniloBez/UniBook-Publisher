package com.unibook.publisher.production.service;

import com.unibook.publisher.production.entity.MediaStream;
import java.io.InputStream;

public interface FileStorageService {

    // binary, stream based
    InputStream get(String fileUrl);

    String put(String path, InputStream content, long size);

    String put(InputStream content, long size);

    // media, content type aware
    MediaStream getMedia(String fileUrl);

    String putMedia(String path, InputStream content, long size);

    // text
    String getAsText(String fileUrl);

    String putText(String path, String content);

    String put(String content);

    boolean exists(String fileUrl);

    void delete(String fileUrl);
}
