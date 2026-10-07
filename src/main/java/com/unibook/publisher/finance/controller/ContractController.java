package com.unibook.publisher.finance.controller;

import com.unibook.publisher.common.enums.UserRole;
import com.unibook.publisher.finance.entity.request.ContractUpdateRequest;
import com.unibook.publisher.finance.entity.request.PayoutSimulationRequest;
import com.unibook.publisher.finance.entity.request.RoyaltyUpdateRequest;
import com.unibook.publisher.finance.entity.response.ContractResponse;
import com.unibook.publisher.finance.entity.response.PayoutSimulationResponse;
import com.unibook.publisher.finance.service.ContractService;
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
@RequestMapping("/api/v1/contracts")
@Tag(name = "Contracts", description = "Author contracts: royalty terms, author confirmation and payout simulation")
public class ContractController {
    private final ContractService contractService;

    public ContractController(ContractService contractService) {
        this.contractService = contractService;
    }

    @GetMapping(params = "manuscriptId")
    @Operation(summary = "Get contract by manuscript ID", description = "Returns the contract linked to the manuscript. Available to the accountant, the administrator and the author who owns the contract")
        @ApiResponse(responseCode = "200", description = "Contract found")
        @ApiResponse(responseCode = "403", description = "The caller is not allowed to view this contract", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
        @ApiResponse(responseCode = "404", description = "Contract for the given manuscript not found", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<ContractResponse> getContract(
            @Parameter(description = "ID of the user making the request (temporary replacement for JWT)", required = true, example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
            @RequestHeader("X-User-Id") UUID userId,
            @Parameter(description = "Role of the user making the request", required = true, example = "ACCOUNTANT")
            @RequestHeader("X-User-Role") UserRole userRole,
            @Parameter(description = "Manuscript ID", required = true, example = "7c9e6679-7425-40de-944b-e07fc1f90ae7")
            @RequestParam UUID manuscriptId
    ) {
        return ResponseEntity.ok(contractService.getContractByManuscriptId(manuscriptId, userId, userRole));
    }

    @PatchMapping("/{id}/royalty")
    @Operation(summary = "Update royalty terms", description = "Changes the royalty percent and advance payment of a DRAFT contract and writes an audit log entry. Resets the author's confirmation. Accountant only")
        @ApiResponse(responseCode = "200", description = "Royalty terms successfully updated")
        @ApiResponse(responseCode = "400", description = "Request validation error", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
        @ApiResponse(responseCode = "403", description = "Only an accountant can change contract terms", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
        @ApiResponse(responseCode = "404", description = "Contract not found", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
        @ApiResponse(responseCode = "422", description = "The contract is not in DRAFT status", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<ContractResponse> updateRoyalty(
            @Parameter(description = "ID of the user making the request (temporary replacement for JWT)", required = true, example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
            @RequestHeader("X-User-Id") UUID userId,
            @Parameter(description = "Role of the user making the request", required = true, example = "ACCOUNTANT")
            @RequestHeader("X-User-Role") UserRole userRole,
            @Parameter(description = "Contract ID", required = true, example = "7c9e6679-7425-40de-944b-e07fc1f90ae7")
            @PathVariable UUID id,
            @RequestBody @Valid RoyaltyUpdateRequest request
    ) {
        return ResponseEntity.ok(contractService.updateRoyalty(id, userId, userRole, request));
    }

    @PostMapping("/{id}/confirm")
    @Operation(summary = "Confirm contract by the author", description = "The author of the manuscript confirms the DRAFT contract. The operation is idempotent: repeated confirmation returns the current contract")
        @ApiResponse(responseCode = "200", description = "Contract confirmed")
        @ApiResponse(responseCode = "403", description = "Only the author of the manuscript can confirm the contract", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
        @ApiResponse(responseCode = "404", description = "Contract not found", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
        @ApiResponse(responseCode = "422", description = "The contract is not in DRAFT status", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<ContractResponse> confirmContract(
            @Parameter(description = "ID of the user making the request (temporary replacement for JWT)", required = true, example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
            @RequestHeader("X-User-Id") UUID userId,
            @Parameter(description = "Contract ID", required = true, example = "7c9e6679-7425-40de-944b-e07fc1f90ae7")
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(contractService.confirmContract(id, userId));
    }

    @PostMapping("/{id}/simulate-payout")
    @Operation(summary = "Simulate author payout", description = "Calculates the expected royalty and total payout for the given sales amount using the selected royalty strategy. Nothing is persisted")
        @ApiResponse(responseCode = "200", description = "Payout successfully calculated")
        @ApiResponse(responseCode = "400", description = "Request validation error", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
        @ApiResponse(responseCode = "403", description = "The caller is not allowed to view this contract", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
        @ApiResponse(responseCode = "404", description = "Contract not found", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
        @ApiResponse(responseCode = "422", description = "Unsupported royalty strategy", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<PayoutSimulationResponse> simulatePayout(
            @Parameter(description = "ID of the user making the request (temporary replacement for JWT)", required = true, example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
            @RequestHeader("X-User-Id") UUID userId,
            @Parameter(description = "Role of the user making the request", required = true, example = "ACCOUNTANT")
            @RequestHeader("X-User-Role") UserRole userRole,
            @Parameter(description = "Contract ID", required = true, example = "7c9e6679-7425-40de-944b-e07fc1f90ae7")
            @PathVariable UUID id,
            @RequestBody @Valid PayoutSimulationRequest request
    ) {
        return ResponseEntity.ok(contractService.simulatePayout(id, userId, userRole, request));
    }


    @GetMapping("/{id}")
    @Operation(summary = "Get contract by ID", description = "Available to the accountant, the administrator and the author who owns the contract")
        @ApiResponse(responseCode = "200", description = "Contract found")
        @ApiResponse(responseCode = "403", description = "The caller is not allowed to view this contract", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
        @ApiResponse(responseCode = "404", description = "Contract not found", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<ContractResponse> getContractById(
            @Parameter(description = "Contract ID", required = true, example = "7c9e6679-7425-40de-944b-e07fc1f90ae7")
            @PathVariable UUID id,
            @Parameter(description = "ID of the user making the request (temporary replacement for JWT)", required = true, example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
            @RequestHeader("X-User-Id") UUID userId,
            @Parameter(description = "Role of the user making the request", required = true, example = "ACCOUNTANT")
            @RequestHeader("X-User-Role") UserRole userRole
    ) {
        return ResponseEntity.ok(contractService.getContractById(id, userId, userRole));
    }

    @GetMapping
    @Operation(summary = "Get all contracts", description = "Returns all contracts. Accountant or administrator only")
        @ApiResponse(responseCode = "200", description = "Contracts successfully retrieved")
        @ApiResponse(responseCode = "403", description = "Only an accountant or an administrator can list all contracts", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<List<ContractResponse>> getAllContracts(
            @Parameter(description = "ID of the user making the request (temporary replacement for JWT)", required = true, example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
            @RequestHeader("X-User-Id") UUID userId,
            @Parameter(description = "Role of the user making the request", required = true, example = "ACCOUNTANT")
            @RequestHeader("X-User-Role") UserRole userRole
    ) {
        return ResponseEntity.ok(contractService.getAllContracts(userId, userRole));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a contract", description = "Replaces royalty percent and advance payment of a DRAFT contract. Resets the author's confirmation. Accountant only")
        @ApiResponse(responseCode = "200", description = "Contract successfully updated")
        @ApiResponse(responseCode = "400", description = "Request validation error", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
        @ApiResponse(responseCode = "403", description = "Only an accountant can update a contract", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
        @ApiResponse(responseCode = "404", description = "Contract not found", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
        @ApiResponse(responseCode = "422", description = "The contract is not in DRAFT status", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<ContractResponse> updateContract(
            @Parameter(description = "Contract ID", required = true, example = "7c9e6679-7425-40de-944b-e07fc1f90ae7")
            @PathVariable UUID id,
            @Parameter(description = "ID of the user making the request (temporary replacement for JWT)", required = true, example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
            @RequestHeader("X-User-Id") UUID userId,
            @Parameter(description = "Role of the user making the request", required = true, example = "ACCOUNTANT")
            @RequestHeader("X-User-Role") UserRole userRole,
            @Valid @RequestBody ContractUpdateRequest request
    ) {
        return ResponseEntity.ok(contractService.updateContract(id, userId, userRole, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a contract", description = "Deletes a contract that is not ACTIVE. Administrator only")
        @ApiResponse(responseCode = "204", description = "Contract successfully deleted")
        @ApiResponse(responseCode = "403", description = "Only an administrator can delete a contract", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
        @ApiResponse(responseCode = "404", description = "Contract not found", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
        @ApiResponse(responseCode = "422", description = "An ACTIVE contract cannot be deleted", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<Void> deleteContract(
            @Parameter(description = "Contract ID", required = true, example = "7c9e6679-7425-40de-944b-e07fc1f90ae7")
            @PathVariable UUID id,
            @Parameter(description = "ID of the user making the request (temporary replacement for JWT)", required = true, example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
            @RequestHeader("X-User-Id") UUID userId,
            @Parameter(description = "Role of the user making the request", required = true, example = "ADMIN")
            @RequestHeader("X-User-Role") UserRole userRole
    ) {
        contractService.deleteContract(id, userId, userRole);
        return ResponseEntity.noContent().build();
    }
}
