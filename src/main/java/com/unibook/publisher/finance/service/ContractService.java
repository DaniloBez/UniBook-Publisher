package com.unibook.publisher.finance.service;

import com.unibook.publisher.common.enums.ContractStatus;
import com.unibook.publisher.common.enums.UserRole;
import com.unibook.publisher.common.event.ContractConfirmedEvent;
import com.unibook.publisher.common.event.ContractRoyaltyUpdatedEvent;
import com.unibook.publisher.common.event.ManuscriptApprovedEvent;
import com.unibook.publisher.common.event.ManuscriptPublishedEvent;
import com.unibook.publisher.common.exception.notfound.ContractNotFoundException;
import com.unibook.publisher.common.exception.security.ForbiddenActionException;
import com.unibook.publisher.common.exception.state.InvalidStateTransitionException;
import com.unibook.publisher.finance.entity.Contract;
import com.unibook.publisher.finance.entity.FinanceAuditLog;
import com.unibook.publisher.finance.entity.request.PayoutSimulationRequest;
import com.unibook.publisher.finance.entity.request.RoyaltyUpdateRequest;
import com.unibook.publisher.finance.entity.response.ContractResponse;
import com.unibook.publisher.finance.entity.response.PayoutSimulationResponse;
import com.unibook.publisher.finance.repository.ContractRepository;
import com.unibook.publisher.finance.repository.FinanceAuditLogRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

@Service
public class ContractService {
    private final ContractRepository contractRepository;
    private final FinanceAuditLogRepository financeAuditLogRepository;
    private final ApplicationEventPublisher eventPublisher;

    public ContractService(
            ContractRepository contractRepository,
            FinanceAuditLogRepository financeAuditLogRepository,
            ApplicationEventPublisher eventPublisher
    ) {
        this.contractRepository = contractRepository;
        this.financeAuditLogRepository = financeAuditLogRepository;
        this.eventPublisher = eventPublisher;
    }

    public void createContractForApprovedManuscript(ManuscriptApprovedEvent event) {
        Contract contract = new Contract(
                null,
                event.manuscriptId(),
                event.manuscriptTitle(),
                event.authorId(),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                ContractStatus.DRAFT,
                null,
                Instant.now()
        );
        contractRepository.save(contract);
    }

    public void activateContractForPublishedManuscript(ManuscriptPublishedEvent event) {
        Contract contract = contractRepository.findByManuscriptId(event.manuscriptId())
                .orElseThrow(() -> new ContractNotFoundException(event.manuscriptId()));

        contractRepository.save(contract.activated());
    }

    public ContractResponse getContractByManuscriptId(UUID manuscriptId, UUID callerId, UserRole callerRole) {
        Contract contract = contractRepository.findByManuscriptId(manuscriptId)
                .orElseThrow(() -> new ContractNotFoundException(manuscriptId));

        checkViewAccess(contract, callerId, callerRole);

        return ContractResponse.from(contract);
    }

    public ContractResponse updateRoyalty(UUID contractId, UUID callerId, UserRole callerRole, RoyaltyUpdateRequest request) {
        if (callerRole != UserRole.ACCOUNTANT)
            throw new ForbiddenActionException("Змінювати умови контракту може тільки бухгалтер");

        Contract contract = getContractOrThrow(contractId);

        if (contract.status() != ContractStatus.DRAFT) {
            throw new InvalidStateTransitionException(
                    "Contract",
                    contractId,
                    contract.status(),
                    ContractStatus.DRAFT,
                    contract.status().allowedTransitions()
            );
        }

        //REM no transactions for now; anyway, no actual db for now as well \(ツ)/
        FinanceAuditLog auditLog = new FinanceAuditLog(
                null,
                contract.id(),
                callerId,
                contract.royaltyPercent(),
                request.newRoyaltyPercent(),
                contract.advancePayment(),
                request.newAdvance(),
                request.reason(),
                Instant.now()
        );
        financeAuditLogRepository.save(auditLog);
        Contract updated = contractRepository.save(
                contract.withUpdatedRoyalty(request.newRoyaltyPercent(), request.newAdvance())
        );

        eventPublisher.publishEvent(new ContractRoyaltyUpdatedEvent(
                updated.id(),
                updated.manuscriptId(),
                updated.manuscriptTitle(),
                updated.authorId(),
                updated.royaltyPercent()
        ));

        return ContractResponse.from(updated);
    }

    public ContractResponse confirmContract(UUID contractId, UUID callerId) {
        Contract contract = getContractOrThrow(contractId);

        if (!contract.authorId().equals(callerId))
            throw new ForbiddenActionException("Підтверджувати контракт може тільки автор рукопису");

        if (contract.status() != ContractStatus.DRAFT) {
            throw new InvalidStateTransitionException(
                    "Contract",
                    contractId,
                    contract.status(),
                    ContractStatus.DRAFT,
                    contract.status().allowedTransitions()
            );
        }

        if (contract.authorConfirmedAt() != null) { //silent idempotency
            return ContractResponse.from(contract);
        }

        Contract updated = contractRepository.save(contract.confirmedByAuthor(Instant.now()));

        eventPublisher.publishEvent(new ContractConfirmedEvent(
                updated.id(),
                updated.manuscriptId(),
                updated.manuscriptTitle(),
                updated.authorId()
        ));

        return ContractResponse.from(updated);
    }

    public PayoutSimulationResponse simulatePayout(UUID contractId, UUID callerId, UserRole callerRole, PayoutSimulationRequest request) {
        Contract contract = getContractOrThrow(contractId);

        checkViewAccess(contract, callerId, callerRole);

        BigDecimal calculatedRoyalty = request.salesAmount()
                .multiply(contract.royaltyPercent())
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        BigDecimal totalPayout = calculatedRoyalty.add(contract.advancePayment());

        return new PayoutSimulationResponse(
                contract.id(),
                request.salesAmount(),
                contract.royaltyPercent(),
                contract.advancePayment(),
                calculatedRoyalty,
                totalPayout
        );
    }

    private Contract getContractOrThrow(UUID contractId) {
        return contractRepository.findById(contractId)
                .orElseThrow(() -> new ContractNotFoundException(contractId));
    }

    private void checkViewAccess(Contract contract, UUID callerId, UserRole callerRole) {
        boolean isOwner = contract.authorId().equals(callerId);
        boolean isPrivileged = callerRole == UserRole.ACCOUNTANT || callerRole == UserRole.ADMIN;

        if (!isOwner && !isPrivileged)
            throw new ForbiddenActionException("Переглядати контракт можуть тільки бухгалтер, адміністратор або автор-власник");
    }
}
