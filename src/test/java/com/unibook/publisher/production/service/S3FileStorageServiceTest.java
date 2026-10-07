package com.unibook.publisher.production.service;

import com.unibook.publisher.common.exception.storage.FileStorageException;
import com.unibook.publisher.common.logging.AppLogger;
import io.minio.*;
import io.minio.errors.ErrorResponseException;
import io.minio.messages.ErrorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
    @DisplayName("Успішне завантаження потоку в MinIO та повернення шляху")
    void put_ShouldUploadStreamAndReturnPath() throws Exception {
        String path = "authorId/manuscriptId/covers/cover.png";
        byte[] content = "fake-image-content".getBytes(StandardCharsets.UTF_8);
        InputStream inputStream = new ByteArrayInputStream(content);

        String result = fileStorageService.put(
                path,
                inputStream,
                content.length,
                "image/png"
        );

        assertEquals(bucket + "/" + path, result);
        verify(minioClient).putObject(any(PutObjectArgs.class));
    }

    @Test
    @DisplayName("Успішне отримання потоку файлу з MinIO")
    void get_ShouldReturnFileStream() throws Exception {
        String path = "authorId/manuscriptId/chapters/ch1/rev1.md";
        GetObjectResponse response = mock(GetObjectResponse.class);

        when(response.readAllBytes()).thenReturn("content".getBytes(StandardCharsets.UTF_8));
        when(minioClient.getObject(any(GetObjectArgs.class))).thenReturn(response);

        InputStream result = fileStorageService.get(path);

        assertNotNull(result);
        assertEquals(
                "content",
                new String(result.readAllBytes(), StandardCharsets.UTF_8)
        );

        verify(minioClient).getObject(any(GetObjectArgs.class));
    }

    @Test
    @DisplayName("Успішна генерація Presigned URL для відображення файлу")
    void getPresignedUrl_ShouldReturnGeneratedUrlForDisplay() throws Exception {
        String path = "authorId/manuscriptId/covers/cover.png";
        String expectedPresignedUrl = "http://localhost:9000/bucket/authorId/manuscriptId/covers/cover.png?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Credential=...";

        when(minioClient.getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class)))
                .thenReturn(expectedPresignedUrl);

        String actualUrl = fileStorageService.getPresignedUrl(path);

        assertNotNull(actualUrl);
        assertEquals(expectedPresignedUrl, actualUrl);
        verify(minioClient).getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class));
    }

    @Test
    @DisplayName("Повертає true, якщо файл існує у сховищі")
    void exists_WhenFileExists_ShouldReturnTrue() throws Exception {
        String path = "file.txt";

        when(minioClient.statObject(any(StatObjectArgs.class))).thenReturn(null);

        boolean result = fileStorageService.exists(path);

        assertTrue(result);
        verify(minioClient).statObject(any(StatObjectArgs.class));
    }

    @Test
    @DisplayName("Повертає false, якщо файл відсутній у сховищі (NoSuchKey)")
    void exists_WhenFileNotFound_ShouldReturnFalse() throws Exception {
        String path = "missing_file.txt";

        ErrorResponse errorResponse = mock(ErrorResponse.class);
        when(errorResponse.code()).thenReturn("NoSuchKey");

        ErrorResponseException exception = mock(ErrorResponseException.class);
        when(exception.errorResponse()).thenReturn(errorResponse);

        when(minioClient.statObject(any(StatObjectArgs.class))).thenThrow(exception);

        boolean result = fileStorageService.exists(path);

        assertFalse(result);
        verify(minioClient).statObject(any(StatObjectArgs.class));
    }

    @Test
    @DisplayName("Кидає FileStorageException у разі помилки підключення до MinIO")
    void exists_WhenStorageThrowsGeneralException_ShouldThrowFileStorageException() throws Exception {
        String path = "file.txt";

        when(minioClient.statObject(any(StatObjectArgs.class)))
                .thenThrow(new RuntimeException("MinIO connection failed"));

        assertThrows(
                FileStorageException.class,
                () -> fileStorageService.exists(path)
        );
    }

    @Test
    @DisplayName("Успішне видалення файлу з MinIO")
    void delete_ShouldRemoveFile() throws Exception {
        String path = "file.txt";

        fileStorageService.delete(path);

        verify(minioClient).removeObject(any(RemoveObjectArgs.class));
    }
}