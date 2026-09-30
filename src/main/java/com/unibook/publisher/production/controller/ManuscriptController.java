package com.unibook.publisher.production.controller;

import com.unibook.publisher.common.enums.UserRole;
import com.unibook.publisher.common.exception.security.ForbiddenActionException;
import com.unibook.publisher.production.entity.request.*;
import com.unibook.publisher.production.enums.ManuscriptStatus;
import com.unibook.publisher.production.entity.response.ManuscriptResponse;
import com.unibook.publisher.production.service.ManuscriptService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/manuscripts")
public class ManuscriptController {
    private final ManuscriptService manuscriptService;

    public ManuscriptController(ManuscriptService manuscriptService) {
        this.manuscriptService = manuscriptService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<ManuscriptResponse> getManuscriptsById(@PathVariable UUID id) {
        ManuscriptResponse manuscriptResponse = manuscriptService.getManuscriptById(id);
        return ResponseEntity.ok(manuscriptResponse);
    }

    @GetMapping
    public ResponseEntity<List<ManuscriptResponse>> getManuscripts(
            @RequestParam(required = false) ManuscriptStatus status
    ) {
        List<ManuscriptResponse> manuscripts = manuscriptService.getManuscripts(status);
        return ResponseEntity.ok(manuscripts);
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<ManuscriptResponse> approveManuscript(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") UUID chiefEditorId,
            @RequestHeader("X-User-Role") UserRole callerRole,
            @Valid @RequestBody ManuscriptApprovalRequest request
            ) {
        if(callerRole != UserRole.CHIEF_EDITOR) {
            throw new ForbiddenActionException("Підтвердити заявку може лише головний редактор");
        }
        ManuscriptResponse response = manuscriptService.approveManuscript(id, chiefEditorId, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<ManuscriptResponse> rejectManuscript(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") UUID chiefEditorId,
            @RequestHeader("X-User-Role") UserRole callerRole,
            @Valid @RequestBody ManuscriptRejectionRequest request
    ) {
        if(callerRole != UserRole.CHIEF_EDITOR) {
            throw new ForbiddenActionException("Відхилити заявку може лише головний редактор");
        }
        ManuscriptResponse response = manuscriptService.rejectManuscript(id, chiefEditorId, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/postpone")
    public ResponseEntity<ManuscriptResponse> postponeManuscript(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") UUID chiefEditorId,
            @RequestHeader("X-User-Role") UserRole callerRole,
            @Valid @RequestBody ManuscriptPostponementRequest request
    ) {
        if(callerRole != UserRole.CHIEF_EDITOR) {
            throw new ForbiddenActionException("Відкласти заявку може лише головний редактор");
        }
        ManuscriptResponse response = manuscriptService.postponeManuscript(id, chiefEditorId, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<ManuscriptResponse> submitManuscript(
            @RequestHeader("X-User-Id") UUID authorId,
            @Valid @RequestBody ManuscriptSubmissionRequest request
    ) {
        ManuscriptResponse response = manuscriptService.submitManuscript(authorId, request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ManuscriptResponse> updateManuscript(
            @PathVariable UUID id,
            @Valid @RequestBody ManuscriptUpdateRequest request
    ) {
        return ResponseEntity.ok(manuscriptService.updateManuscript(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteManuscript(@PathVariable UUID id) {
        manuscriptService.deleteManuscript(id);
        return ResponseEntity.noContent().build();
    }
}
