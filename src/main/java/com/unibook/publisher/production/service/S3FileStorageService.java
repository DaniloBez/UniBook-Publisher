package com.unibook.publisher.production.service;

import com.unibook.publisher.common.exception.notfound.FileNotFoundException;
import com.unibook.publisher.common.exception.storage.FileStorageException;
import com.unibook.publisher.common.logging.AppLogger;
import com.unibook.publisher.production.entity.MediaStream;

import io.minio.*;
import io.minio.errors.ErrorResponseException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Service
public class S3FileStorageService implements FileStorageService {

    private static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";
    private static final long UNKNOWN_SIZE_PART_SIZE = 10L * 1024 * 1024;

    private final AppLogger logger;
    private final MinioClient client;
    private final String bucket;

    public S3FileStorageService(
            AppLogger logger,
            MinioClient client,
            @Value("${s3.bucket}") String bucket
    ) {
        this.logger = logger;
        this.client = client;
        this.bucket = bucket;
    }

    @Override
    public InputStream get(String fileUrl) {
        return fetch(fileUrl);
    }

    @Override
    public MediaStream getMedia(String fileUrl) {
        GetObjectResponse response = fetch(fileUrl);

        String contentType = response.headers().get("Content-Type");
        String length = response.headers().get("Content-Length");

        return new MediaStream(
                response,
                contentType != null ? contentType : DEFAULT_CONTENT_TYPE,
                length != null ? Long.parseLong(length) : -1L
        );
    }

    @Override
    public String getAsText(String fileUrl) {
        try (InputStream inputStream = get(fileUrl)) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            logger.error("Failed to read file {}", e, fileUrl);
            throw new FileStorageException(fileUrl);
        }
    }

    @Override
    public String put(String path, InputStream content, long size) {
        return upload(path, content, size, DEFAULT_CONTENT_TYPE);
    }

    @Override
    public String put(InputStream content, long size) {
        return put(
                UUID.randomUUID().toString(),
                content,
                size
        );
    }

    @Override
    public String putMedia(String path, InputStream content, long size) {
        String contentType = MediaTypeFactory.getMediaType(path)
                .map(MediaType::toString)
                .orElse(DEFAULT_CONTENT_TYPE);

        return upload(path, content, size, contentType);
    }

    @Override
    public String putText(String path, String content) {
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);

        return upload(
                path,
                new ByteArrayInputStream(bytes),
                bytes.length,
                DEFAULT_CONTENT_TYPE
        );
    }

    @Override
    public String put(String content) {
        return putText(
                UUID.randomUUID().toString() + ".txt",
                content
        );
    }

    @Override
    public boolean exists(String fileUrl) {
        try {
            client.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucket)
                            .object(extractObjectKey(fileUrl))
                            .build()
            );

            return true;

        } catch (ErrorResponseException e) {
            if ("NoSuchKey".equals(e.errorResponse().code())) {
                return false;
            }

            throw resolveStorageException(fileUrl, e);

        } catch (Exception e) {
            throw resolveStorageException(fileUrl, e);
        }
    }

    @Override
    public void delete(String fileUrl) {
        try {
            client.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucket)
                            .object(extractObjectKey(fileUrl))
                            .build()
            );

        } catch (Exception e) {
            logger.error("Failed to delete file {}", e, fileUrl);
            throw resolveStorageException(fileUrl, e);
        }
    }

    private GetObjectResponse fetch(String fileUrl) {
        try {
            return client.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucket)
                            .object(extractObjectKey(fileUrl))
                            .build()
            );

        } catch (Exception e) {
            logger.error("Failed to get file {}", e, fileUrl);
            throw resolveStorageException(fileUrl, e);
        }
    }

    private String upload(
            String path,
            InputStream content,
            long size,
            String contentType
    ) {
        try {
            long partSize = size >= 0 ? -1L : UNKNOWN_SIZE_PART_SIZE;

            client.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucket)
                            .object(path)
                            .stream(content, size, partSize)
                            .contentType(contentType)
                            .build()
            );

            return buildUrl(path);

        } catch (Exception e) {
            logger.error("Failed to put file {}", e, path);
            throw new FileStorageException(path);
        }
    }

    private RuntimeException resolveStorageException(String fileUrl, Exception e) {
        if (e instanceof ErrorResponseException error) {
            if ("NoSuchKey".equals(error.errorResponse().code())) {
                return new FileNotFoundException(fileUrl);
            }
        }

        return new FileStorageException(fileUrl);
    }

    private String buildUrl(String object) {
        return bucket + "/" + object;
    }

    private String extractObjectKey(String url) {
        String prefix = bucket + "/";

        if (url.startsWith(prefix)) {
            return url.substring(prefix.length());
        }

        // allow passing raw object keys too
        return url;
    }
}
