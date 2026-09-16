package com.unibook.publisher.production.controller;

import com.unibook.publisher.production.entity.request.CoverVersionRequest;
import com.unibook.publisher.production.entity.response.CoverVersionResponse;
import com.unibook.publisher.production.service.CoverVersionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
            @Valid @RequestBody CoverVersionRequest request
    ) {
        return ResponseEntity.ok(coverVersionService.uploadCoverVersion(id, designerId, request));
    }

    @GetMapping("/{id}/cover-versions")
    public ResponseEntity<List<CoverVersionResponse>> getCoverVersions(@PathVariable UUID id) {
        return ResponseEntity.ok(coverVersionService.getCoverVersions(id));
    }
}
