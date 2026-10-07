package com.unibook.publisher.production.controller;

import com.unibook.publisher.common.exception.badrequest.FileIsEmptyException;
import com.unibook.publisher.common.exception.storage.FileReadException;
import com.unibook.publisher.production.entity.response.CoverVersionResponse;
import com.unibook.publisher.production.service.CoverVersionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/manuscripts")
public class CoverVersionController {
    private final CoverVersionService coverVersionService;

    public CoverVersionController(CoverVersionService coverVersionService) {
        this.coverVersionService = coverVersionService;
    }

    @PostMapping("/{id}/cover-versions")
    public ResponseEntity<CoverVersionResponse> uploadCoverVersion(
            @PathVariable UUID id,
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
    public ResponseEntity<List<CoverVersionResponse>> getCoverVersions(@PathVariable UUID id) {
        return ResponseEntity.ok(coverVersionService.getCoverVersions(id));
    }
}
