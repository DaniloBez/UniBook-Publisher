package com.unibook.publisher.production.service;

import com.unibook.publisher.common.logging.AppLogger;
import com.unibook.publisher.common.exception.notfound.FileNotFoundException;
import com.unibook.publisher.common.exception.storage.FileStorageException;

import io.minio.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class S3FileStorageServiceTest {

    @Mock
    private AppLogger logger;

    @Mock
    private MinioClient minioClient;

    private S3FileStorageService fileStorageService;

    private final String bucket = "bucket";

    @BeforeEach
    void setUp() {
        fileStorageService = new S3FileStorageService(
                logger,
                minioClient,
                bucket
        );
    }


    @Test
    void putText_ShouldUploadTextAndReturnUrl() throws Exception {
        String path = "test/file.txt";
        String content = "Hello S3";

        String url = fileStorageService.putText(path, content);

        assertEquals(
                bucket + "/" + path,
                url
        );

        verify(minioClient)
                .putObject(any(PutObjectArgs.class));
    }


    @Test
    void put_ShouldUploadBytesAndReturnUrl() throws Exception {
        byte[] content = "Hello".getBytes(StandardCharsets.UTF_8);

        String url = fileStorageService.put(
                "test/file.bin",
                new ByteArrayInputStream(content),
                content.length
        );

        assertEquals(
                bucket + "/test/file.bin",
                url
        );

        verify(minioClient)
                .putObject(any(PutObjectArgs.class));
    }


    @Test
    void putTextWithoutPath_ShouldGenerateRandomTextFileUrl() throws Exception {
        String url = fileStorageService.put("Hello world");

        assertTrue(
                url.startsWith(bucket + "/")
        );

        assertTrue(
                url.endsWith(".txt")
        );

        verify(minioClient)
                .putObject(any(PutObjectArgs.class));
    }


    @Test
    void get_ShouldReturnFileStream() throws Exception {
    GetObjectResponse response = mock(GetObjectResponse.class);

    when(response.readAllBytes()).thenReturn("content".getBytes(StandardCharsets.UTF_8));

    when(minioClient.getObject(any(GetObjectArgs.class))).thenReturn(response);

        InputStream result = fileStorageService.get(
                bucket + "/file.txt"
        );

        assertEquals(
                "content",
                new String(
                        result.readAllBytes(),
                        StandardCharsets.UTF_8
                )
        );

        verify(minioClient)
                .getObject(any(GetObjectArgs.class));
    }


    @Test
    void exists_WhenFileExists_ShouldReturnTrue() throws Exception {
        when(minioClient.statObject(any(StatObjectArgs.class)))
                .thenReturn(null);

        boolean result = fileStorageService.exists(
                bucket + "/file.txt"
        );

        assertTrue(result);

        verify(minioClient).statObject(any(StatObjectArgs.class));
    }


    @Test
    void exists_WhenStorageThrowsException_ShouldThrowException() throws Exception {
        when(minioClient.statObject(any(StatObjectArgs.class)))
                .thenThrow(new FileStorageException("missing_file"));

        assertThrows(
                FileStorageException.class,
                () -> fileStorageService.exists(
                        bucket + "/file.txt"
                )
        );
    }


    @Test
    void delete_ShouldRemoveFile() throws Exception {
        fileStorageService.delete(
                bucket + "/file.txt"
        );

        verify(minioClient)
                .removeObject(any(RemoveObjectArgs.class));
    }
}
