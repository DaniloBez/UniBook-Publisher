package com.unibook.publisher.production.controller;

import com.unibook.publisher.common.enums.UserRole;
import com.unibook.publisher.common.exception.ForbiddenActionException;
import com.unibook.publisher.production.entity.request.AssignDesignerRequest;
import com.unibook.publisher.production.entity.response.AuditLogResponse;
import com.unibook.publisher.production.entity.response.ManuscriptResponse;
import com.unibook.publisher.production.service.ManuscriptFinalizationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/manuscripts")
public class ManuscriptEditorialController {
    private final ManuscriptFinalizationService finalizationService;

    public ManuscriptEditorialController(ManuscriptFinalizationService finalizationService) {
        this.finalizationService = finalizationService;
    }

    @PostMapping("/{id}/finalize-text")
    public ResponseEntity<ManuscriptResponse> finalizeText(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") UUID editorId
    ) {
        return ResponseEntity.ok(finalizationService.finalizeText(id, editorId));
    }

    @PostMapping("/{id}/assign-designer")
    public ResponseEntity<ManuscriptResponse> assignDesigner(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") UUID chiefEditorId,
            @RequestHeader("X-User-Role") UserRole callerRole,
            @Valid @RequestBody AssignDesignerRequest request
    ) {
        if (callerRole != UserRole.CHIEF_EDITOR) {
            throw new ForbiddenActionException("Призначати дизайнера може лише головний редактор");
        }
        return ResponseEntity.ok(finalizationService.assignDesigner(id, chiefEditorId, request));
    }

    @PostMapping("/{id}/publish")
    public ResponseEntity<ManuscriptResponse> publish(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") UUID chiefEditorId,
            @RequestHeader("X-User-Role") UserRole callerRole
    ) {
        if (callerRole != UserRole.CHIEF_EDITOR) {
            throw new ForbiddenActionException("Публікувати рукопис може лише головний редактор");
        }
        return ResponseEntity.ok(finalizationService.publish(id, chiefEditorId));
    }

    @GetMapping("/{id}/audit-log")
    public ResponseEntity<List<AuditLogResponse>> getAuditLog(@PathVariable UUID id) {
        return ResponseEntity.ok(finalizationService.getAuditLog(id));
    }
}
