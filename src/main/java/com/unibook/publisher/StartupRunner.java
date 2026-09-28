package com.unibook.publisher;

import com.unibook.publisher.common.logging.AppLogger;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.errors.ErrorResponseException;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;


@Component
public class StartupRunner implements CommandLineRunner {

    private final AppLogger logger;
    private final MinioClient client;
    private final String bucket;

    public StartupRunner(AppLogger logger, MinioClient client, @Value("${s3.bucket}") String bucket) {
        this.logger = logger;
        this.client = client;
        this.bucket = bucket;
    }

    @Override
    public void run(String @NonNull ... args) { 
        logger.info("Startup logger test: {}", "Hello from Spring");
        touchTimestamp();
    }

    @SuppressWarnings("java:S1141")
    private void touchTimestamp() { //To healthcheck remote S3
        logger.info("Trying to reach S3");
        String object = "timestamp.txt";
        String content = null;

        try {
            try {
                client.statObject(
                        StatObjectArgs.builder()
                                .bucket(bucket)
                                .object(object)
                                .build()
                );

                try (var stream = client.getObject(
                        GetObjectArgs.builder()
                                .bucket(bucket)
                                .object(object)
                                .build()
                )) {
                    content = new String(
                            stream.readAllBytes(),
                            StandardCharsets.UTF_8
                    );
                }

                logger.info("Existing timestamp: {}", content);

            } catch (ErrorResponseException e) {
                if ("NoSuchKey".equals(e.errorResponse().code())) {
                    logger.info("{} does not exist. Creating...", object);
                } else {
                    throw e;
                }
            }

            content = Instant.now().toString();

            logger.info("New timestamp: {}", content);

            byte[] data = content.getBytes(StandardCharsets.UTF_8);

            client.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucket)
                            .object(object)
                            .stream(
                                    new ByteArrayInputStream(data),
                                    (long) data.length,
                                    -1L
                            )
                            .contentType("text/plain")
                            .build()
            );

            logger.info("{} uploaded successfully - S3 functions as expected", object);

        } catch (Exception e) {
            logger.error("Failed to update {}: {} - No S3 functionality available", object, e.getMessage());
        }
    }
}
