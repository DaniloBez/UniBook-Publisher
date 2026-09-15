package com.unibook.publisher.production.entity;

import java.util.UUID;

public record Genre(
        UUID genreId,
        String name
) {}