package com.unibook.publisher.storage.internal;

import com.unibook.publisher.common.logging.AppLogger;
import com.unibook.publisher.storage.FileStorageService;
import io.minio.MinioClient;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class FileStorageAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
                    .withConfiguration(AutoConfigurations.of(FileStorageAutoConfiguration.class))
                    .withBean(AppLogger.class, () -> Mockito.mock(AppLogger.class));

    @Test
    void shouldRegisterLocalStorageService() {
        runner.withPropertyValues("file-storage.enabled=true", "file-storage.type=LOCAL", "file-storage.location=./test-upload")
                .run(context -> {
                    assertThat(context).hasSingleBean(FileStorageService.class);
                    assertThat(context.getBean(FileStorageService.class))
                            .isInstanceOf(LocalFileStorageService.class);}
                );
    }

    @Test
    void shouldRegisterS3StorageService() {
        runner.withPropertyValues(
                "file-storage.enabled=true",
                        "file-storage.type=S3",
                        "s3.bucket=publisher",
                        "s3.endpoint=http://localhost:9000",
                        "s3.access-key=test",
                        "s3.secret-key=test",
                        "s3.region=us-east-1"
                )
                .withBean(
                        MinioClient.class,
                        () -> MinioClient.builder()
                                .endpoint("http://localhost:9000")
                                .credentials("test", "test")
                                .build()
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(FileStorageService.class);
                    assertThat(context.getBean(FileStorageService.class))
                            .isInstanceOf(S3FileStorageService.class);
                });
    }
}