package com.unibook.publisher.storage;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "file-storage")
@Validated
public record FileStorageProperties (
    boolean enabled,

    @NotNull
    StorageType type,

    String location,

    String serverUrl
)
{}
