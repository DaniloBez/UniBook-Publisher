package com.unibook.publisher.production.controller;

import com.unibook.publisher.production.entity.request.GenreRequest;
import com.unibook.publisher.production.entity.response.GenreResponse;
import com.unibook.publisher.production.service.GenreService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("api/v1/genres")
@Tag(name = "Genres", description = "Directory of literary genres used to classify manuscripts")
public class GenreController {
    private final GenreService genreService;

    public GenreController(GenreService genreService) {
        this.genreService = genreService;
    }

    @GetMapping()
    @Operation(summary = "Get all genres")
    @ApiResponse(responseCode = "200", description = "Genres successfully retrieved")
    public ResponseEntity<List<GenreResponse>> getAllGenres() {
        return ResponseEntity.ok(genreService.getAllGenres());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get genre by ID")
        @ApiResponse(responseCode = "200", description = "Genre found")
        @ApiResponse(responseCode = "404", description = "Genre not found", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<GenreResponse> getGenreById(
            @Parameter(description = "Genre ID", required = true, example = "5e8f2a4c-1b3d-4c6e-9f7a-0d2b4c6e8f1a")
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(genreService.getGenreById(id));
    }

    @GetMapping("/search")
    @Operation(summary = "Find genre by name", description = "Returns the genre with the exact given name")
        @ApiResponse(responseCode = "200", description = "Genre found")
        @ApiResponse(responseCode = "404", description = "Genre with this name not found", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<GenreResponse> getGenreByName(
            @Parameter(description = "Genre name", required = true, example = "Science fiction")
            @RequestParam String name
    ) {
        return ResponseEntity.ok(genreService.getGenreByName(name));
    }

    @PostMapping
    @Operation(summary = "Create a genre")
        @ApiResponse(responseCode = "201", description = "Genre successfully created")
        @ApiResponse(responseCode = "400", description = "Request validation error", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
        @ApiResponse(responseCode = "409", description = "A genre with this name already exists", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<GenreResponse> createGenre(@Valid @RequestBody GenreRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(genreService.createGenre(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a genre", description = "Renames the genre. The new name must not be used by another genre")
        @ApiResponse(responseCode = "200", description = "Genre successfully updated")
        @ApiResponse(responseCode = "400", description = "Request validation error", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
        @ApiResponse(responseCode = "404", description = "Genre not found", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
        @ApiResponse(responseCode = "409", description = "A genre with this name already exists", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<GenreResponse> updateGenre(
            @Parameter(description = "Genre ID", required = true, example = "5e8f2a4c-1b3d-4c6e-9f7a-0d2b4c6e8f1a")
            @PathVariable UUID id,
            @Valid @RequestBody GenreRequest request
    ) {
        return ResponseEntity.ok(genreService.updateGenre(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a genre")
        @ApiResponse(responseCode = "204", description = "Genre successfully deleted")
        @ApiResponse(responseCode = "404", description = "Genre not found", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<GenreResponse> deleteGenre(
            @Parameter(description = "Genre ID", required = true, example = "5e8f2a4c-1b3d-4c6e-9f7a-0d2b4c6e8f1a")
            @PathVariable UUID id
    ) {
        genreService.deleteGenre(id);
        return ResponseEntity.noContent().build();
    }
}
