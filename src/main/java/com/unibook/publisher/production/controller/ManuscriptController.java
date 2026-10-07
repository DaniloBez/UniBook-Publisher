package com.unibook.publisher.production.controller;

import com.unibook.publisher.common.enums.UserRole;
import com.unibook.publisher.common.exception.security.ForbiddenActionException;
import com.unibook.publisher.production.entity.request.*;
import com.unibook.publisher.production.enums.ManuscriptStatus;
import com.unibook.publisher.production.entity.response.ManuscriptResponse;
import com.unibook.publisher.production.service.ManuscriptService;
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
@Tag(name = "Manuscripts", description = "Manuscript submission and the chief editor's decisions on submitted manuscripts")
public class ManuscriptController {
    private final ManuscriptService manuscriptService;

    public ManuscriptController(ManuscriptService manuscriptService) {
        this.manuscriptService = manuscriptService;
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get manuscript by ID", description = "Returns the manuscript data including its genres")
        @ApiResponse(responseCode = "200", description = "Manuscript found")
        @ApiResponse(responseCode = "404", description = "Manuscript not found", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<ManuscriptResponse> getManuscriptsById(
            @Parameter(description = "Manuscript ID", required = true, example = "7c9e6679-7425-40de-944b-e07fc1f90ae7")
            @PathVariable UUID id
    ) {
        ManuscriptResponse manuscriptResponse = manuscriptService.getManuscriptById(id);
        return ResponseEntity.ok(manuscriptResponse);
    }

    @GetMapping
    @Operation(summary = "Get manuscripts", description = "Returns all manuscripts, optionally filtered by status")
    @ApiResponse(responseCode = "200", description = "Manuscripts successfully retrieved")
    public ResponseEntity<List<ManuscriptResponse>> getManuscripts(
            @Parameter(description = "Filter: return only manuscripts with this status", example = "SUBMITTED")
            @RequestParam(required = false) ManuscriptStatus status
    ) {
        List<ManuscriptResponse> manuscripts = manuscriptService.getManuscripts(status);
        return ResponseEntity.ok(manuscripts);
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "Approve a manuscript", description = "Moves the manuscript to IN_PROGRESS, assigns the editor and triggers contract creation. Chief editor only")
        @ApiResponse(responseCode = "200", description = "Manuscript approved")
        @ApiResponse(responseCode = "400", description = "Request validation error", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
        @ApiResponse(responseCode = "403", description = "Only the chief editor can approve a manuscript", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
        @ApiResponse(responseCode = "404", description = "Manuscript not found", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
        @ApiResponse(responseCode = "422", description = "The transition to IN_PROGRESS is not allowed from the current status", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<ManuscriptResponse> approveManuscript(
            @Parameter(description = "Manuscript ID", required = true, example = "7c9e6679-7425-40de-944b-e07fc1f90ae7")
            @PathVariable UUID id,
            @Parameter(description = "ID of the chief editor making the request (temporary replacement for JWT)", required = true, example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
            @RequestHeader("X-User-Id") UUID chiefEditorId,
            @Parameter(description = "Role of the user making the request", required = true, example = "CHIEF_EDITOR")
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
    @Operation(summary = "Reject a manuscript", description = "Moves the manuscript to REJECTED and notifies the author with the reason. Chief editor only")
        @ApiResponse(responseCode = "200", description = "Manuscript rejected")
        @ApiResponse(responseCode = "400", description = "Request validation error", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
        @ApiResponse(responseCode = "403", description = "Only the chief editor can reject a manuscript", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
        @ApiResponse(responseCode = "404", description = "Manuscript not found", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
        @ApiResponse(responseCode = "422", description = "The transition to REJECTED is not allowed from the current status", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<ManuscriptResponse> rejectManuscript(
            @Parameter(description = "Manuscript ID", required = true, example = "7c9e6679-7425-40de-944b-e07fc1f90ae7")
            @PathVariable UUID id,
            @Parameter(description = "ID of the chief editor making the request (temporary replacement for JWT)", required = true, example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
            @RequestHeader("X-User-Id") UUID chiefEditorId,
            @Parameter(description = "Role of the user making the request", required = true, example = "CHIEF_EDITOR")
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
    @Operation(summary = "Postpone a manuscript", description = "Moves the manuscript to POSTPONED and notifies the author with a comment. Chief editor only")
        @ApiResponse(responseCode = "200", description = "Manuscript postponed")
        @ApiResponse(responseCode = "400", description = "Request validation error", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
        @ApiResponse(responseCode = "403", description = "Only the chief editor can postpone a manuscript", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
        @ApiResponse(responseCode = "404", description = "Manuscript not found", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
        @ApiResponse(responseCode = "422", description = "The transition to POSTPONED is not allowed from the current status", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<ManuscriptResponse> postponeManuscript(
            @Parameter(description = "Manuscript ID", required = true, example = "7c9e6679-7425-40de-944b-e07fc1f90ae7")
            @PathVariable UUID id,
            @Parameter(description = "ID of the chief editor making the request (temporary replacement for JWT)", required = true, example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
            @RequestHeader("X-User-Id") UUID chiefEditorId,
            @Parameter(description = "Role of the user making the request", required = true, example = "CHIEF_EDITOR")
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
    @Operation(summary = "Submit a manuscript", description = "Registers a new manuscript from the author with status SUBMITTED")
        @ApiResponse(responseCode = "200", description = "Manuscript successfully submitted")
        @ApiResponse(responseCode = "400", description = "Request validation error", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<ManuscriptResponse> submitManuscript(
            @Parameter(description = "ID of the author making the request (temporary replacement for JWT)", required = true, example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
            @RequestHeader("X-User-Id") UUID authorId,
            @Valid @RequestBody ManuscriptSubmissionRequest request
    ) {
        ManuscriptResponse response = manuscriptService.submitManuscript(authorId, request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a manuscript", description = "Replaces the title, annotation, draft file and genres of the manuscript")
        @ApiResponse(responseCode = "200", description = "Manuscript successfully updated")
        @ApiResponse(responseCode = "400", description = "Request validation error", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
        @ApiResponse(responseCode = "404", description = "Manuscript or one of the genres not found", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<ManuscriptResponse> updateManuscript(
            @Parameter(description = "Manuscript ID", required = true, example = "7c9e6679-7425-40de-944b-e07fc1f90ae7")
            @PathVariable UUID id,
            @Valid @RequestBody ManuscriptUpdateRequest request
    ) {
        return ResponseEntity.ok(manuscriptService.updateManuscript(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a manuscript")
        @ApiResponse(responseCode = "204", description = "Manuscript successfully deleted")
        @ApiResponse(responseCode = "404", description = "Manuscript not found", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<Void> deleteManuscript(
            @Parameter(description = "Manuscript ID", required = true, example = "7c9e6679-7425-40de-944b-e07fc1f90ae7")
            @PathVariable UUID id
    ) {
        manuscriptService.deleteManuscript(id);
        return ResponseEntity.noContent().build();
    }
}
