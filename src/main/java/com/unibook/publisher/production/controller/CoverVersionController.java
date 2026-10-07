package com.unibook.publisher.production.controller;

import com.unibook.publisher.common.exception.badrequest.FileIsEmptyException;
import com.unibook.publisher.common.exception.storage.FileReadException;
import com.unibook.publisher.production.entity.response.CoverVersionResponse;
import com.unibook.publisher.production.service.CoverVersionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/manuscripts")
@Tag(name = "Cover versions", description = "Cover design versions uploaded by the assigned designer")
public class CoverVersionController {
    private final CoverVersionService coverVersionService;

    public CoverVersionController(CoverVersionService coverVersionService) {
        this.coverVersionService = coverVersionService;
    }

    @PostMapping("/{id}/cover-versions")
    @Operation(summary = "Upload a cover version", description = "Adds a new cover version to a manuscript that is IN_DESIGN. Only the designer assigned to the manuscript can do this")
        @ApiResponse(responseCode = "200", description = "Cover version successfully uploaded")
        @ApiResponse(responseCode = "400", description = "Request validation error", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
        @ApiResponse(responseCode = "403", description = "No designer is assigned, or the caller is not the assigned designer", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
        @ApiResponse(responseCode = "404", description = "Manuscript not found", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
        @ApiResponse(responseCode = "422", description = "The manuscript is not in IN_DESIGN status", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<CoverVersionResponse> uploadCoverVersion(
            @Parameter(description = "Manuscript ID", required = true, example = "7c9e6679-7425-40de-944b-e07fc1f90ae7")
            @PathVariable UUID id,
            @Parameter(description = "ID of the designer making the request (temporary replacement for JWT)", required = true, example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
            @RequestHeader("X-User-Id") UUID designerId,
            @RequestParam MultipartFile file
    ) {
        if (file.isEmpty())
            throw new FileIsEmptyException();

        try {
            return ResponseEntity.ok(coverVersionService.uploadCoverVersion(
                    id,
                    designerId,
                    file.getInputStream(),
                    file.getSize(),
                    file.getContentType(),
                    file.getOriginalFilename()
            ));
        } catch (IOException _) {
            System.out.printf("Проблема читання файлу " + file.getOriginalFilename());
            throw new FileReadException(file.getOriginalFilename());
        }
    }

    @GetMapping("/{id}/cover-versions")
    @Operation(summary = "Get cover versions", description = "Returns all cover versions of the manuscript ordered by version number ascending")
        @ApiResponse(responseCode = "200", description = "Cover versions successfully retrieved")
        @ApiResponse(responseCode = "404", description = "Manuscript not found", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<List<CoverVersionResponse>> getCoverVersions(
            @Parameter(description = "Manuscript ID", required = true, example = "7c9e6679-7425-40de-944b-e07fc1f90ae7")
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(coverVersionService.getCoverVersions(id));
    }
}
