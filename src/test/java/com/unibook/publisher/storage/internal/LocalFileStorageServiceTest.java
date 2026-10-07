package com.unibook.publisher.storage.internal;

import com.unibook.publisher.common.exception.notfound.FileNotFoundException;
import com.unibook.publisher.common.exception.storage.FileStorageException;
import com.unibook.publisher.common.logging.AppLogger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class LocalFileStorageServiceTest {
    @TempDir
    Path tempDir;

    private LocalFileStorageService storageService;
    private AppLogger logger;

    @BeforeEach
    void setUp() {
        logger = Mockito.mock(AppLogger.class);
        FileStorageProperties properties = new FileStorageProperties(
                true,
                StorageType.LOCAL,
                tempDir.toString(),
                "http://localhost:8080"
        );
        storageService = new LocalFileStorageService(logger, properties);
    }

    @Test
    void put_Success() throws IOException {
        String path = "file.txt";
        String content = "Hello, World!";
        storageService.put(
                path,
                new ByteArrayInputStream(content.getBytes()),
                content.length(),
                "text/plain"
        );
        assertTrue(storageService.exists(path));
        Path saved = tempDir.resolve(path);
        assertTrue(Files.exists(saved));
        assertEquals(content, Files.readString(saved));
    }

//    @Test
//    void put_FileStorageException() {
//        String path = "../outside/file.txt";
//        assertThrows(FileStorageException.class, () ->
//                storageService.put(
//                        path,
//                        new ByteArrayInputStream("test".getBytes()),
//                        4,
//                        "text/plain"
//                )
//        );
//    }

    @Test
    void get_Success() throws IOException {
        String path = "file.txt";
        String content = "Hello, World!";

        Files.writeString(
                tempDir.resolve(path),
                content
        );

        try (InputStream is = storageService.get(path)) {
            String result = new String(is.readAllBytes());
            assertEquals(content, result);
        }
    }

    @Test
    void get_FileNotFoundException() {
        String path = "file.txt";
        assertThrows(FileNotFoundException.class, () -> storageService.get(path));
    }

    @Test
    void exists_Exists() throws IOException {
        String path = "file.txt";
        Files.writeString(tempDir.resolve(path), "data");
        assertTrue(storageService.exists(path));
    }

    @Test
    void exists_NotExists() throws IOException {
        String path = "missing/file.txt";
        assertFalse(storageService.exists(path));
    }

    @Test
    void delete_Success() throws IOException {
        String path = "file.txt";
        Files.writeString(tempDir.resolve(path), "data");
        storageService.delete(path);
        assertFalse(storageService.exists(path));
    }

    @Test
    void delete_FileStorageException() {
        String path = "../outside/file.txt";
        assertThrows(FileStorageException.class, () -> storageService.delete(path));
    }

    @Test
    void getPresignedUrl_Success() {
        String path = "image.png";
        String result = storageService.getPresignedUrl(path);
        assertEquals(
                "http://localhost:8080/api/v1/storage/raw?path=image.png",
                result
        );
    }

    @Test
    void constructor_FileStorageException() throws IOException {
        Path file = tempDir.resolve("storage");
        Files.writeString(file, "not directory");

        FileStorageProperties properties =
                new FileStorageProperties(
                        true,
                        StorageType.LOCAL,
                        file.toString(),
                        "http://localhost:8080"
                );
        assertThrows(FileStorageException.class, () -> new LocalFileStorageService(logger, properties));
    }

    @Test
    void constructor_DefaultLocation_Success() {
        FileStorageProperties properties =
                new FileStorageProperties(
                        true,
                        StorageType.LOCAL,
                        null,
                        "http://localhost:8080"
                );

        LocalFileStorageService service = new LocalFileStorageService(logger, properties);
        assertNotNull(service);
    }

    @Test
    void get_FileStorageException() throws IOException {
        String path = "folder";
        Files.createDirectory(tempDir.resolve(path));
        assertThrows(FileStorageException.class, () -> storageService.get(path));
    }

    @Test
    void getPresignedUrl_DefaultServerUrl_Success() {
        FileStorageProperties properties =
                new FileStorageProperties(
                        true,
                        StorageType.LOCAL,
                        tempDir.toString(),
                        null
                );

        LocalFileStorageService service =
                new LocalFileStorageService(
                        logger,
                        properties
                );
        String result = service.getPresignedUrl("file.txt");
        assertEquals("http://localhost:8080/api/v1/storage/raw?path=file.txt", result);
    }
}
