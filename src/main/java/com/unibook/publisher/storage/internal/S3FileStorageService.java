package com.unibook.publisher.storage.internal;

import com.unibook.publisher.common.exception.notfound.FileNotFoundException;
import com.unibook.publisher.common.exception.storage.FileStorageException;
import com.unibook.publisher.common.logging.AppLogger;

import com.unibook.publisher.storage.FileStorageService;
import io.minio.*;
import io.minio.errors.ErrorResponseException;
import org.springframework.beans.factory.annotation.Value;

import java.io.InputStream;
import java.util.concurrent.TimeUnit;

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
    public String getPresignedUrl(String path) {
        try {
            return client.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Http.Method.GET)
                            .bucket(bucket)
                            .object(extractObjectKey(path))
                            .expiry(30, TimeUnit.MINUTES)
                            .build()
            );
        } catch (Exception e) {
            logger.error("Не вдалося створити попередньо підписану URL-адресу для файлу {}", path, e);
            throw resolveStorageException(path, e);
        }
    }

    @Override
    public String put(String path, InputStream content, long size, String contentType) {
        String resolvedContentType = contentType != null ? contentType : DEFAULT_CONTENT_TYPE;
        return upload(path, content, size, resolvedContentType);
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
