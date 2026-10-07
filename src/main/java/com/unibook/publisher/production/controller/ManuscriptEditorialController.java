package com.unibook.publisher.production.controller;

import com.unibook.publisher.common.enums.UserRole;
import com.unibook.publisher.common.exception.security.ForbiddenActionException;
import com.unibook.publisher.production.entity.request.AssignDesignerRequest;
import com.unibook.publisher.production.entity.response.ManuscriptAuditLogResponse;
import com.unibook.publisher.production.entity.response.ManuscriptResponse;
import com.unibook.publisher.production.service.ManuscriptFinalizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/manuscripts")
@Tag(name = "Manuscript editorial workflow", description = "Final stages of the manuscript lifecycle: text approval, designer assignment, publication and audit log")
public class ManuscriptEditorialController {
    private final ManuscriptFinalizationService finalizationService;

    public ManuscriptEditorialController(ManuscriptFinalizationService finalizationService) {
        this.finalizationService = finalizationService;
    }

    @PostMapping("/{id}/finalize-text")
    @Operation(summary = "Finalize the manuscript text", description = "Moves the manuscript to TEXT_APPROVED. Only the assigned editor can do this, and only when there are no open threads or pending suggestions")
        @ApiResponse(responseCode = "200", description = "Text finalized")
        @ApiResponse(responseCode = "403", description = "No editor is assigned, or the caller is not the assigned editor", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
        @ApiResponse(responseCode = "404", description = "Manuscript not found", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
        @ApiResponse(responseCode = "422", description = "The transition is not allowed from the current status, or unresolved threads or suggestions remain", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<ManuscriptResponse> finalizeText(
            @Parameter(description = "Manuscript ID", required = true, example = "7c9e6679-7425-40de-944b-e07fc1f90ae7")
            @PathVariable UUID id,
            @Parameter(description = "ID of the editor making the request (temporary replacement for JWT)", required = true, example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
            @RequestHeader("X-User-Id") UUID editorId
    ) {
        return ResponseEntity.ok(finalizationService.finalizeText(id, editorId));
    }

    @PostMapping("/{id}/assign-designer")
    @Operation(summary = "Assign a designer", description = "Assigns a cover designer and moves the manuscript to IN_DESIGN. Chief editor only")
        @ApiResponse(responseCode = "200", description = "Designer assigned")
        @ApiResponse(responseCode = "400", description = "Request validation error", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
        @ApiResponse(responseCode = "403", description = "Only the chief editor can assign a designer", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
        @ApiResponse(responseCode = "404", description = "Manuscript not found", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
        @ApiResponse(responseCode = "422", description = "The transition to IN_DESIGN is not allowed from the current status", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<ManuscriptResponse> assignDesigner(
            @Parameter(description = "Manuscript ID", required = true, example = "7c9e6679-7425-40de-944b-e07fc1f90ae7")
            @PathVariable UUID id,
            @Parameter(description = "ID of the chief editor making the request (temporary replacement for JWT)", required = true, example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
            @RequestHeader("X-User-Id") UUID chiefEditorId,
            @Parameter(description = "Role of the user making the request", required = true, example = "CHIEF_EDITOR")
            @RequestHeader("X-User-Role") UserRole callerRole,
            @Valid @RequestBody AssignDesignerRequest request
    ) {
        if (callerRole != UserRole.CHIEF_EDITOR) {
            throw new ForbiddenActionException("Призначати дизайнера може лише головний редактор");
        }
        return ResponseEntity.ok(finalizationService.assignDesigner(id, chiefEditorId, request));
    }

    @PostMapping("/{id}/publish")
    @Operation(summary = "Publish a manuscript", description = "Moves the manuscript to PUBLISHED and activates its contract. Requires at least one uploaded cover version. Chief editor only")
        @ApiResponse(responseCode = "200", description = "Manuscript published")
        @ApiResponse(responseCode = "403", description = "Only the chief editor can publish a manuscript", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
        @ApiResponse(responseCode = "404", description = "Manuscript not found", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
        @ApiResponse(responseCode = "422", description = "The transition to PUBLISHED is not allowed from the current status, or no cover version exists", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<ManuscriptResponse> publish(
            @Parameter(description = "Manuscript ID", required = true, example = "7c9e6679-7425-40de-944b-e07fc1f90ae7")
            @PathVariable UUID id,
            @Parameter(description = "ID of the chief editor making the request (temporary replacement for JWT)", required = true, example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
            @RequestHeader("X-User-Id") UUID chiefEditorId,
            @Parameter(description = "Role of the user making the request", required = true, example = "CHIEF_EDITOR")
            @RequestHeader("X-User-Role") UserRole callerRole
    ) {
        if (callerRole != UserRole.CHIEF_EDITOR) {
            throw new ForbiddenActionException("Публікувати рукопис може лише головний редактор");
        }
        return ResponseEntity.ok(finalizationService.publish(id, chiefEditorId));
    }

    @GetMapping("/{id}/audit-log")
    @Operation(summary = "Get the manuscript audit log", description = "Returns the history of manuscript status changes with the user who made each change")
        @ApiResponse(responseCode = "200", description = "Audit log successfully retrieved")
        @ApiResponse(responseCode = "404", description = "Manuscript not found", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<List<ManuscriptAuditLogResponse>> getAuditLog(
            @Parameter(description = "Manuscript ID", required = true, example = "7c9e6679-7425-40de-944b-e07fc1f90ae7")
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(finalizationService.getAuditLog(id));
    }
}
