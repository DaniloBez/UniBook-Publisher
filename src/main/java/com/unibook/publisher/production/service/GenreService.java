package com.unibook.publisher.production.service;

import com.unibook.publisher.production.entity.request.GenreRequest;
import com.unibook.publisher.production.entity.response.GenreResponse;

import java.util.List;
import java.util.UUID;

public interface GenreService {
    List<GenreResponse> getAllGenres();
    GenreResponse getGenreById(UUID id);
    GenreResponse getGenreByName(String name);
    GenreResponse createGenre(GenreRequest request);
    GenreResponse updateGenre(UUID id, GenreRequest request);
    void deleteGenre(UUID id);
}
