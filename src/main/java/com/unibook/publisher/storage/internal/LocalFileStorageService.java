package com.unibook.publisher.storage.internal;

import com.unibook.publisher.common.exception.storage.FileStorageException;
import com.unibook.publisher.common.exception.notfound.FileNotFoundException;
import com.unibook.publisher.common.logging.AppLogger;
import com.unibook.publisher.storage.FileStorageService;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class LocalFileStorageService implements FileStorageService {
    private final AppLogger logger;
    private final FileStorageProperties properties;
    private final Path root;

    public LocalFileStorageService(AppLogger logger, FileStorageProperties properties) {
        this.logger = logger;
        this.properties = properties;
        String location = properties.location() != null ?  properties.location() : "./uploads";
        this.root = Paths.get(location).toAbsolutePath().normalize();

        try {
            Files.createDirectories(this.root);
        } catch (IOException e) {
            logger.error("Помилка при створенні директорії локального сховища {}", location, e);
            throw new FileStorageException(location, e);
        }
        logger.info("Ініціалізація LocalFileStorageService за шляхом {}", this.root);
    }

    @Override
    public InputStream get(String fileUrl) {
        Path file = resolve(fileUrl);
        if (!Files.exists(file)) {
            throw new FileNotFoundException(fileUrl);
        }
        try {
            return Files.newInputStream(file);
        } catch (IOException e) {
            logger.error("Помилка при читанні файлу {}", fileUrl, e);
            throw new FileStorageException(fileUrl, e);
        }
    }

    @Override
    public String getPresignedUrl(String path) {
        String serverUrl = properties.serverUrl() != null ? properties.serverUrl() : "http://localhost:8080";
        return serverUrl + "/api/v1/storage/raw?path=" + path;
    }

    @Override
    public String put(String path, InputStream content, long size, String contentType) {
        try {
            Path file = resolve(path);
            Files.createDirectories(file.getParent());
            Files.copy(content, file, StandardCopyOption.REPLACE_EXISTING);
            return path;
        } catch (IOException e) {
            logger.error("Помилка при збереженні файлу {}", path, e);
            throw new FileStorageException(path, e);
        }
    }

    @Override
    public boolean exists(String fileUrl) {
        return Files.exists(resolve(fileUrl));
    }

    @Override
    public void delete(String fileUrl) {
        try {
            Files.deleteIfExists(resolve(fileUrl));
        } catch (IOException e) {
            logger.error("Помилка при видаленні файлу {}", fileUrl, e);
            throw new FileStorageException(fileUrl, e);
        }
    }

    private Path resolve(String fileUrl) {
        Path resolved = root.resolve(fileUrl).normalize();
        if(!resolved.startsWith(root)) {
            throw new FileStorageException(fileUrl);
        }
        return resolved;
    }
}
