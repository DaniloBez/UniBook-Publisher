package com.unibook.publisher.production.entity.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.unibook.publisher.production.entity.Genre;

import java.util.UUID;

public record GenreResponse(
        @JsonProperty("genre_id")
        UUID genreId,

        @JsonProperty("genreName")
        String genreName
) {
    public static GenreResponse from(Genre genre) {
        return new GenreResponse(
                genre.getGenreId(),
                genre.getGenreName()
        );
    }
}
