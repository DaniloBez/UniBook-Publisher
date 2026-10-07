package com.unibook.publisher.storage.internal;

import com.unibook.publisher.common.logging.AppLogger;
import com.unibook.publisher.storage.FileStorageService;
import io.minio.MinioClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnClass(FileStorageService.class)
@EnableConfigurationProperties(FileStorageProperties.class)
@ConditionalOnProperty(prefix = "file-storage", name = "enabled", havingValue = "true", matchIfMissing = false)
public class FileStorageAutoConfiguration {
    private static final Logger log = LoggerFactory.getLogger(FileStorageAutoConfiguration.class);

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "file-storage", name = "type", havingValue = "LOCAL", matchIfMissing = true)
    public FileStorageService localFileStorageService(AppLogger logger, FileStorageProperties properties) {
        log.info("Реєстрація LocalFileStorageService з автоконфігурації");
        return new LocalFileStorageService(logger, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "file-storage", name = "type", havingValue = "S3")
    public FileStorageService s3FileStorageService(AppLogger logger, MinioClient minioClient, @Value("${s3.bucket}") String bucket) {
        log.info("Реєстрація S3FileStorageService з автоконфігурації");
        return new S3FileStorageService(logger, minioClient, bucket);
    }
}
