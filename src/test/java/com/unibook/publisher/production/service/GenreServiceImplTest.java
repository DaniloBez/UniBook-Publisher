package com.unibook.publisher.production.service;

import com.unibook.publisher.common.exception.conflict.GenreAlreadyExistsException;
import com.unibook.publisher.common.exception.notfound.GenreNotFoundException;
import com.unibook.publisher.common.logging.AppLogger;
import com.unibook.publisher.production.entity.Genre;
import com.unibook.publisher.production.entity.request.GenreRequest;
import com.unibook.publisher.production.entity.response.GenreResponse;
import com.unibook.publisher.production.repository.GenreRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class GenreServiceImplTest {
    @Mock
    private GenreRepository genreRepository;

    @Mock
    private AppLogger logger;

    @InjectMocks
    private GenreServiceImpl genreService;

    @Test
    void getAllGenres_Success() {
        Genre genre = new Genre(UUID.randomUUID(), "Фантастика");
        when(genreRepository.findAll()).thenReturn(List.of(genre));
        List<GenreResponse> genres = genreService.getAllGenres();

        assertThat(genres).hasSize(1);
        assertThat(genres.get(0).genreName()).isEqualTo("Фантастика");
    }

    @Test
    void getGenreById_Success() {
        UUID id = UUID.randomUUID();
        Genre genre = new Genre(id, "Фантастика");
        when(genreRepository.findById(id)).thenReturn(Optional.of(genre));
        GenreResponse response = genreService.getGenreById(id);

        assertThat(response.genreId()).isEqualTo(id);
        assertThat(response.genreName()).isEqualTo("Фантастика");
    }

    @Test
    void getGenreById_ThrowsGenreNotFoundException() {
        UUID id = UUID.randomUUID();
        when(genreRepository.findById(id)).thenReturn(Optional.empty());
        assertThrows(GenreNotFoundException.class, () -> genreService.getGenreById(id));
    }

    @Test
    void getGenreByName_Success() {
        UUID id = UUID.randomUUID();
        Genre genre = new Genre(id, "Фантастика");
        when(genreRepository.findByGenreName("Фантастика")).thenReturn(Optional.of(genre));
        GenreResponse response = genreService.getGenreByName("Фантастика");

        assertThat(response.genreId()).isEqualTo(id);
        assertThat(response.genreName()).isEqualTo("Фантастика");
    }

    @Test
    void getGenreByName_ThrowsGenreNotFoundException() {
        when(genreRepository.findByGenreName("Невідомо")).thenReturn(Optional.empty());
        assertThrows(GenreNotFoundException.class, () -> genreService.getGenreByName("Невідомо"));
    }

    @Test
    void createGenre_Success() {
        UUID id = UUID.randomUUID();
        GenreRequest request = new GenreRequest("Фантастика");
        when(genreRepository.existsByGenreName("Фантастика")).thenReturn(false);
        when(genreRepository.save(any(Genre.class))).thenReturn(new Genre(id, "Фантастика"));
        GenreResponse response = genreService.createGenre(request);

        assertThat(response.genreId()).isEqualTo(id);
        assertThat(response.genreName()).isEqualTo("Фантастика");
        verify(genreRepository).save(any(Genre.class));
    }

    @Test
    void createGenre_ThrowsGenreAlreadyExistsException() {
        GenreRequest request = new GenreRequest("Фантастика");
        when(genreRepository.existsByGenreName("Фантастика")).thenReturn(true);
        assertThrows(GenreAlreadyExistsException.class, () -> genreService.createGenre(request));
        verify(genreRepository, never()).save(any());
    }

    @Test
    void updateGenre_Success() {
        UUID id = UUID.randomUUID();
        Genre genre = new Genre(id, "Фантастика");
        GenreRequest request = new GenreRequest("Наукова фантастика");

        when(genreRepository.findById(id)).thenReturn(Optional.of(genre));
        when(genreRepository.existsByGenreName("Наукова фантастика")).thenReturn(false);
        when(genreRepository.save(any(Genre.class))).thenAnswer(invocation -> invocation.getArgument(0));
        GenreResponse response = genreService.updateGenre(id, request);

        assertThat(response.genreName()).isEqualTo("Наукова фантастика");
    }

    @Test
    void updateGenre_ThrowsGenreNotFoundException() {
        UUID id = UUID.randomUUID();
        GenreRequest request = new GenreRequest("Жанр");

        when(genreRepository.findById(id)).thenReturn(Optional.empty());
        assertThrows(GenreNotFoundException.class, () -> genreService.updateGenre(id, request));
        verify(genreRepository, never()).save(any());
    }

    @Test
    void updateGenre_ThrowsGenreAlreadyExistsException_NotExistsByGenreName() {
        UUID id = UUID.randomUUID();
        Genre genre = new Genre(id, "Фантастика");
        GenreRequest request = new GenreRequest("Детектив");

        when(genreRepository.findById(id)).thenReturn(Optional.of(genre));
        when(genreRepository.existsByGenreName("Детектив")).thenReturn(true);
        assertThrows(GenreAlreadyExistsException.class, () -> genreService.updateGenre(id, request));
        verify(genreRepository, never()).save(any());
    }

    @Test
    void updateGenre_SameNameDifferentCase_Equeals() {
        UUID id = UUID.randomUUID();
        Genre genre = new Genre(id, "Фантастика");
        GenreRequest request = new GenreRequest("фантастика");

        when(genreRepository.findById(id)).thenReturn(Optional.of(genre));
        when(genreRepository.save(any(Genre.class))).thenAnswer(invocation -> invocation.getArgument(0));
        GenreResponse response = genreService.updateGenre(id, request);
        assertThat(response.genreName()).isEqualTo("фантастика");
        verify(genreRepository, never()).existsByGenreName(any());
    }

    @Test
    void deleteGenre_Success() {
        UUID id = UUID.randomUUID();
        when(genreRepository.existsById(id)).thenReturn(true);
        genreService.deleteGenre(id);
        verify(genreRepository).deleteById(id);
    }


    @Test
    void deleteGenre_ThrowsGenreNotFoundException() {
        UUID id = UUID.randomUUID();
        when(genreRepository.existsById(id)).thenReturn(false);
        assertThrows(GenreNotFoundException.class, () -> genreService.deleteGenre(id));
        verify(genreRepository, never()).deleteById(any());
    }
}
