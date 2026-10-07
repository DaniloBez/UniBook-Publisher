package com.unibook.publisher.storage.internal;

import com.unibook.publisher.common.exception.notfound.FileNotFoundException;
import com.unibook.publisher.common.exception.storage.FileStorageException;
import com.unibook.publisher.common.logging.AppLogger;

import io.minio.*;
import io.minio.errors.ErrorResponseException;
import io.minio.messages.ErrorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class S3FileStorageServiceTest {
    private S3FileStorageService storageService;
    private MinioClient client;
    private AppLogger logger;

    @BeforeEach
    void setUp() {
        client = Mockito.mock(MinioClient.class);
        logger = Mockito.mock(AppLogger.class);

        storageService = new S3FileStorageService(
                logger,
                client,
                "publisher"
        );
    }

    @Test
    void put_Success() throws Exception {
        when(client.putObject(Mockito.any(PutObjectArgs.class))).thenReturn(Mockito.mock(ObjectWriteResponse.class));

        String result = storageService.put(
                "file.txt",
                new ByteArrayInputStream("test".getBytes()),
                4,
                "text/plain"
        );
        assertEquals("publisher/file.txt", result);
    }

    @Test
    void put_DefaultContentType_Success() throws Exception {
        when(client.putObject(Mockito.any(PutObjectArgs.class))).thenReturn(Mockito.mock(ObjectWriteResponse.class));

        String result = storageService.put(
                "file.txt",
                new ByteArrayInputStream("test".getBytes()),
                4,
                null
        );
        assertEquals("publisher/file.txt", result);
    }

    @Test
    void put_SizeLessThan0_Success() throws Exception {
        when(client.putObject(Mockito.any(PutObjectArgs.class))).thenReturn(Mockito.mock(ObjectWriteResponse.class));

        String result = storageService.put(
                "file.txt",
                new ByteArrayInputStream("test".getBytes()),
                -1,
                "text/plain"
        );
        assertEquals("publisher/file.txt", result);
    }

    @Test
    void put_FileStorageException() throws Exception {
        when(client.putObject(Mockito.any(PutObjectArgs.class))).thenThrow(new RuntimeException());

        assertThrows(
                FileStorageException.class,
                () -> storageService.put(
                        "file.txt",
                        new ByteArrayInputStream("test".getBytes()),
                        4,
                        "text/plain"
                )
        );
    }

    @Test
    void get_Success() throws Exception {
        GetObjectResponse response = Mockito.mock(GetObjectResponse.class);
        when(client.getObject(Mockito.any(GetObjectArgs.class))).thenReturn(response);
        InputStream result = storageService.get("file.txt");
        assertEquals(response, result);
    }

    @Test
    void get_FileStorageException() throws Exception {
        ErrorResponseException exception = Mockito.mock(ErrorResponseException.class);
        ErrorResponse error = Mockito.mock(ErrorResponse.class);

        when(exception.errorResponse()).thenReturn(error);
        when(error.code()).thenReturn("NoSuchKey");
        when(client.getObject(Mockito.any(GetObjectArgs.class))).thenThrow(exception);
        assertThrows(FileNotFoundException.class, () -> storageService.get("file.txt"));
    }

    @Test
    void exists_Success() throws Exception {
        when(client.statObject(Mockito.any(StatObjectArgs.class))).thenReturn(Mockito.mock(StatObjectResponse.class));
        assertTrue(storageService.exists("file.txt"));
    }

    @Test
    void exists_ErrorResponseException() throws Exception {
        ErrorResponseException exception = Mockito.mock(ErrorResponseException.class);
        ErrorResponse error = Mockito.mock(ErrorResponse.class);

        when(exception.errorResponse()).thenReturn(error);
        when(error.code()).thenReturn("NoSuchKey");
        when(client.statObject(Mockito.any(StatObjectArgs.class))).thenThrow(exception);
        assertFalse(storageService.exists("file.txt"));
    }

    @Test
    void exists_FileStorageException_WhenNotNoSuchKey() throws Exception {
        ErrorResponseException exception = Mockito.mock(ErrorResponseException.class);
        ErrorResponse error = Mockito.mock(ErrorResponse.class);

        when(exception.errorResponse()).thenReturn(error);
        when(error.code()).thenReturn("OtherError");
        when(client.statObject(Mockito.any(StatObjectArgs.class))).thenThrow(exception);
        assertThrows(FileStorageException.class, () -> storageService.exists("file.txt"));
    }

    @Test
    void exists_FileStorageException_WhenRuntimeException() throws Exception {
        when(client.statObject(Mockito.any(StatObjectArgs.class))).thenThrow(new RuntimeException());
        assertThrows(FileStorageException.class, () -> storageService.exists("file.txt"));
    }

    @Test
    void delete_Success() {
        assertDoesNotThrow(() -> storageService.delete("file.txt"));
    }

    @Test
    void delete_FileStorageException() throws Exception {
        Mockito.doThrow(new RuntimeException()).when(client).removeObject(Mockito.any(RemoveObjectArgs.class));
        assertThrows(FileStorageException.class, () -> storageService.delete("file.txt"));
    }

    @Test
    void getPresignedUrl_Success() throws Exception {
        when(client.getPresignedObjectUrl(Mockito.any(GetPresignedObjectUrlArgs.class))).thenReturn("url");
        assertEquals("url", storageService.getPresignedUrl("file.txt"));
    }

    @Test
    void getPresignedUrl_FileStorageException() throws Exception {
        when(client.getPresignedObjectUrl(Mockito.any(GetPresignedObjectUrlArgs.class))).thenThrow(new RuntimeException());
        assertThrows(FileStorageException.class, () -> storageService.getPresignedUrl("file.txt"));
    }

    @Test
    void getPresignedUrl_WithBucketPrefix_Success() throws Exception {
        when(client.getPresignedObjectUrl(Mockito.any(GetPresignedObjectUrlArgs.class))).thenReturn("url");
        String result = storageService.getPresignedUrl("publisher/images/test.png");
        assertEquals("url", result);
    }
}