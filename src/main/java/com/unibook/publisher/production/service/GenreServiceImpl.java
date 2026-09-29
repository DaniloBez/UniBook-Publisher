package com.unibook.publisher.production.service;

import com.unibook.publisher.common.exception.conflict.GenreAlreadyExistsException;
import com.unibook.publisher.common.exception.notfound.GenreNotFoundException;
import com.unibook.publisher.common.logging.AppLogger;
import com.unibook.publisher.production.entity.Genre;
import com.unibook.publisher.production.entity.request.GenreRequest;
import com.unibook.publisher.production.entity.response.GenreResponse;
import com.unibook.publisher.production.repository.GenreRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class GenreServiceImpl implements GenreService {
    private final GenreRepository genreRepository;
    private final AppLogger logger;

    public GenreServiceImpl(GenreRepository genreRepository, AppLogger logger) {
        this.genreRepository = genreRepository;
        this.logger = logger;
    }

    @Override
    public List<GenreResponse> getAllGenres() {
        return genreRepository.findAll().stream()
                .map(GenreResponse::from)
                .toList();
    }

    @Override
    public GenreResponse getGenreById(UUID id) {
        Genre genre = genreRepository.findById(id)
                .orElseThrow(() -> new GenreNotFoundException(id));
        return GenreResponse.from(genre);
    }

    @Override
    public GenreResponse getGenreByName(String name) {
        Genre genre = genreRepository.findByGenreName(name)
                .orElseThrow(() -> new GenreNotFoundException(name));
        return GenreResponse.from(genre);
    }

    @Override
    @Transactional
    public GenreResponse createGenre(GenreRequest request) {
        if(genreRepository.existsByGenreName(request.genreName())) {
            throw new GenreAlreadyExistsException(request.genreName());
        }
        Genre genre = new Genre();
        genre.setGenreName(request.genreName());
        Genre saved = genreRepository.save(genre);

        logger.info(
                "Created new genre: '{}' with id {}",
                saved.getGenreName(),
                saved.getGenreId()
        );
        return GenreResponse.from(saved);
    }

    @Override
    @Transactional
    public GenreResponse updateGenre(UUID id, GenreRequest request) {
        Genre genre = genreRepository.findById(id)
                .orElseThrow(() -> new GenreNotFoundException(id));

        if (!genre.getGenreName().equalsIgnoreCase(request.genreName()) && genreRepository.existsByGenreName(request.genreName())) {
            throw new GenreAlreadyExistsException(request.genreName());
        }

        String oldGenreName = genre.getGenreName();
        genre.setGenreName(request.genreName());
        Genre updated = genreRepository.save(genre);

        logger.info(
                "Updated genre {}: '{}' -> '{}'",
                updated.getGenreId(),
                oldGenreName,
                updated.getGenreName()
        );
        return GenreResponse.from(updated);
    }

    @Override
    @Transactional
    public void deleteGenre(UUID id) {
        if (!genreRepository.existsById(id)) {
            throw new GenreNotFoundException(id);
        }
        genreRepository.deleteById(id);
        logger.info("Deleted genre with id {}", id);
    }
}
