package com.unibook.publisher.finance.controller;

import com.unibook.publisher.common.enums.UserRole;
import com.unibook.publisher.finance.entity.request.ContractCreateRequest;
import com.unibook.publisher.finance.entity.request.ContractUpdateRequest;
import com.unibook.publisher.finance.entity.request.PayoutSimulationRequest;
import com.unibook.publisher.finance.entity.request.RoyaltyUpdateRequest;
import com.unibook.publisher.finance.entity.response.ContractResponse;
import com.unibook.publisher.finance.entity.response.PayoutSimulationResponse;
import com.unibook.publisher.finance.service.ContractService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/contracts")
public class ContractController {
    private final ContractService contractService;

    public ContractController(ContractService contractService) {
        this.contractService = contractService;
    }

    @GetMapping
    public ResponseEntity<ContractResponse> getContract(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader("X-User-Role") UserRole userRole,
            @RequestParam UUID manuscriptId
    ) {
        return ResponseEntity.ok(contractService.getContractByManuscriptId(manuscriptId, userId, userRole));
    }

    @PatchMapping("/{id}/royalty")
    public ResponseEntity<ContractResponse> updateRoyalty(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader("X-User-Role") UserRole userRole,
            @PathVariable UUID id,
            @RequestBody @Valid RoyaltyUpdateRequest request
    ) {
        return ResponseEntity.ok(contractService.updateRoyalty(id, userId, userRole, request));
    }

    @PostMapping("/{id}/confirm")
    public ResponseEntity<ContractResponse> confirmContract(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(contractService.confirmContract(id, userId));
    }

    @PostMapping("/{id}/simulate-payout")
    public ResponseEntity<PayoutSimulationResponse> simulatePayout(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader("X-User-Role") UserRole userRole,
            @PathVariable UUID id,
            @RequestBody @Valid PayoutSimulationRequest request
    ) {
        return ResponseEntity.ok(contractService.simulatePayout(id, userId, userRole, request));
    }

    @PostMapping
    public ResponseEntity<ContractResponse> createContract(
            @Valid @RequestBody ContractCreateRequest request
    ) {
        return ResponseEntity.status(201).body(contractService.createContract(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ContractResponse> getContractById(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader("X-User-Role") UserRole userRole
    ) {
        return ResponseEntity.ok(contractService.getContractById(id, userId, userRole));
    }

    @GetMapping
    public ResponseEntity<List<ContractResponse>> getAllContracts(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader("X-User-Role") UserRole userRole
    ) {
        return ResponseEntity.ok(contractService.getAllContracts(userId, userRole));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ContractResponse> updateContract(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader("X-User-Role") UserRole userRole,
            @Valid @RequestBody ContractUpdateRequest request
    ) {
        return ResponseEntity.ok(contractService.updateContract(id, userId, userRole, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteContract(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader("X-User-Role") UserRole userRole
    ) {
        contractService.deleteContract(id, userId, userRole);
        return ResponseEntity.noContent().build();
    }
}
