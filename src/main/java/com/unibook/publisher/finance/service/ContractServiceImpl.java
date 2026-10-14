package com.unibook.publisher.finance.service;

import com.unibook.publisher.common.enums.ContractStatus;
import com.unibook.publisher.common.enums.UserRole;
import com.unibook.publisher.common.enums.RoyaltyStrategyType;
import com.unibook.publisher.common.event.ContractConfirmedEvent;
import com.unibook.publisher.common.event.ContractRoyaltyUpdatedEvent;
import com.unibook.publisher.common.event.ManuscriptApprovedEvent;
import com.unibook.publisher.common.event.ManuscriptPublishedEvent;
import com.unibook.publisher.common.exception.business.UnsupportedRoyaltyStrategyException;
import com.unibook.publisher.common.exception.notfound.ContractNotFoundException;
import com.unibook.publisher.common.exception.security.ForbiddenActionException;
import com.unibook.publisher.common.exception.state.InvalidStateTransitionException;
import com.unibook.publisher.common.logging.AppLogger;
import com.unibook.publisher.finance.entity.Contract;
import com.unibook.publisher.finance.entity.FinanceAuditLog;
import com.unibook.publisher.finance.entity.request.ContractUpdateRequest;
import com.unibook.publisher.finance.entity.request.PayoutSimulationRequest;
import com.unibook.publisher.finance.entity.request.RoyaltyUpdateRequest;
import com.unibook.publisher.finance.entity.response.ContractResponse;
import com.unibook.publisher.finance.entity.response.PayoutSimulationResponse;
import com.unibook.publisher.finance.repository.ContractRepository;
import com.unibook.publisher.finance.repository.FinanceAuditLogRepository;
import com.unibook.publisher.finance.royalty.RoyaltyStrategy;
import com.unibook.publisher.identity.entity.User;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ContractServiceImpl implements ContractService {
    private final ContractRepository contractRepository;
    private final FinanceAuditLogRepository financeAuditLogRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final AppLogger logger;
    private final Map<RoyaltyStrategyType, RoyaltyStrategy> strategies;

    public ContractServiceImpl(
            ContractRepository contractRepository,
            FinanceAuditLogRepository financeAuditLogRepository,
            ApplicationEventPublisher eventPublisher,
            AppLogger logger,
            List<RoyaltyStrategy> strategyList
    ) {
        this.contractRepository = contractRepository;
        this.financeAuditLogRepository = financeAuditLogRepository;
        this.eventPublisher = eventPublisher;
        this.logger = logger;
        this.strategies = strategyList.stream().collect(Collectors.toMap(RoyaltyStrategy::getType, Function.identity()));
    }

    @Override
    @Transactional
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

        logger.info(
            "Created contract for manuscript {} by author {}",
            event.manuscriptId(),
            event.authorId()
        );
    }

    @Override
    @Transactional
    public void activateContractForPublishedManuscript(ManuscriptPublishedEvent event) {
        Contract contract = contractRepository.findByManuscriptId(event.manuscriptId())
                .orElseThrow(() -> new ContractNotFoundException(event.manuscriptId()));

        ContractStatus oldStatus = contract.getStatus();
        contract.setStatus(ContractStatus.ACTIVE);
        contractRepository.save(contract);

        logger.info(
            "Updated contract {} status: {} -> {}",
            contract.getId(),
            oldStatus,
            contract.getStatus()
        );
    }

    @Override
    public ContractResponse getContractByManuscriptId(UUID manuscriptId, User currentUser) {
        Contract contract = contractRepository.findByManuscriptId(manuscriptId)
                .orElseThrow(() -> new ContractNotFoundException(manuscriptId));

        checkViewAccess(contract, currentUser);

        return ContractResponse.from(contract);
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ACCOUNTANT')")
    public ContractResponse updateRoyalty(UUID contractId, User currentUser, RoyaltyUpdateRequest request) {
        UUID callerId = currentUser.getId();

        Contract contract = getContractOrThrow(contractId);

        if (contract.getStatus() != ContractStatus.DRAFT) {
            throw new InvalidStateTransitionException(
                    "Contract",
                    contractId,
                    contract.getStatus(),
                    ContractStatus.DRAFT,
                    contract.getStatus().allowedTransitions()
            );
        }

        FinanceAuditLog auditLog = new FinanceAuditLog(
                null,
                contract,
                callerId,
                contract.getRoyaltyPercent(),
                request.newRoyaltyPercent(),
                contract.getAdvancePayment(),
                request.newAdvance(),
                request.reason(),
                Instant.now()
        );
        financeAuditLogRepository.save(auditLog);

        BigDecimal oldRoyalty = contract.getRoyaltyPercent();
        BigDecimal oldAdvance = contract.getAdvancePayment();

        contract.setRoyaltyPercent(request.newRoyaltyPercent());
        contract.setAdvancePayment(request.newAdvance());
        contract.setAuthorConfirmedAt(null);
        Contract updated = contractRepository.save(contract);

        logger.info(
            "Updated contract {} royalty: {} -> {}, advance: {} -> {}, by user {}",
            contractId,
            oldRoyalty,
            updated.getRoyaltyPercent(),
            oldAdvance,
            updated.getAdvancePayment(),
            callerId
        );

        eventPublisher.publishEvent(new ContractRoyaltyUpdatedEvent(
                updated.getId(),
                updated.getManuscriptId(),
                updated.getTitle(),
                updated.getAuthorId(),
                updated.getRoyaltyPercent()
        ));

        return ContractResponse.from(updated);
    }

    @Override
    @Transactional
    public ContractResponse confirmContract(UUID contractId, User currentUser) {
        UUID callerId = currentUser.getId();

        Contract contract = getContractOrThrow(contractId);

        if (!contract.getAuthorId().equals(callerId))
            throw new ForbiddenActionException("Підтверджувати контракт може тільки автор рукопису");

        if (contract.getStatus() != ContractStatus.DRAFT) {
            throw new InvalidStateTransitionException(
                    "Contract",
                    contractId,
                    contract.getStatus(),
                    ContractStatus.DRAFT,
                    contract.getStatus().allowedTransitions()
            );
        }

        if (contract.getAuthorConfirmedAt() != null) { //silent idempotency
            return ContractResponse.from(contract);
        }

        contract.setAuthorConfirmedAt(Instant.now());
        Contract updated = contractRepository.save(contract);

        logger.info(
            "Updated contract {}: author {} confirmed contract",
            contractId,
            callerId
        );

        eventPublisher.publishEvent(new ContractConfirmedEvent(
                updated.getId(),
                updated.getManuscriptId(),
                updated.getTitle(),
                updated.getAuthorId()
        ));

        return ContractResponse.from(updated);
    }

    @Override
    public PayoutSimulationResponse simulatePayout(UUID contractId, User currentUser, PayoutSimulationRequest request) {
        Contract contract = getContractOrThrow(contractId);

        checkViewAccess(contract, currentUser);

        RoyaltyStrategy strategy = resolveStrategy(request.royaltyStrategyType());

        BigDecimal calculatedRoyalty = strategy.calculateRoyalty(
                contract,
                request.salesAmount()
        );

        BigDecimal totalPayout = calculatedRoyalty.add(contract.getAdvancePayment());

        return new PayoutSimulationResponse(
                contract.getId(),
                request.salesAmount(),
                contract.getRoyaltyPercent(),
                contract.getAdvancePayment(),
                calculatedRoyalty,
                totalPayout
        );
    }

    private Contract getContractOrThrow(UUID contractId) {
        return contractRepository.findById(contractId)
                .orElseThrow(() -> new ContractNotFoundException(contractId));
    }

    private void checkViewAccess(Contract contract, User currentUser) {
        boolean isOwner = contract.getAuthorId().equals(currentUser.getId());
        boolean isPrivileged = hasAnyRole(currentUser, UserRole.ACCOUNTANT, UserRole.ADMIN);

        if (!isOwner && !isPrivileged)
            throw new ForbiddenActionException("Переглядати контракт можуть тільки бухгалтер, адміністратор або автор-власник");
    }

    private boolean hasAnyRole(User user, UserRole... roles) {
        Set<String> authorities = user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        return Arrays.stream(roles).anyMatch(role -> authorities.contains("ROLE_" + role.name()));
    }

    private RoyaltyStrategy resolveStrategy(RoyaltyStrategyType type) {
        RoyaltyStrategy strategy = strategies.get(type);

        if (strategy == null) {
            throw new UnsupportedRoyaltyStrategyException(type);
        }

        return strategy;
    }

    @Override
    public ContractResponse getContractById(UUID id, User currentUser) {
        Contract contract = getContractOrThrow(id);
        checkViewAccess(contract, currentUser);
        return ContractResponse.from(contract);
    }

    @Override
    @PreAuthorize("hasAnyRole('ACCOUNTANT', 'ADMIN')")
    public List<ContractResponse> getAllContracts() {
        return contractRepository.findAll().stream()
                .map(ContractResponse::from)
                .toList();
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ACCOUNTANT')")
    public ContractResponse updateContract(UUID id, ContractUpdateRequest request) {
        Contract contract = getContractOrThrow(id);

        if (contract.getStatus() != ContractStatus.DRAFT) {
            throw new InvalidStateTransitionException(
                    "Contract", id, contract.getStatus(), ContractStatus.DRAFT,
                    contract.getStatus().allowedTransitions()
            );
        }

        contract.setRoyaltyPercent(request.royaltyPercent());
        contract.setAdvancePayment(request.advancePayment());
        contract.setAuthorConfirmedAt(null);

        Contract updated = contractRepository.save(contract);
        logger.info("Updated contract {}: royalty={}, advance={}",
                id, updated.getRoyaltyPercent(), updated.getAdvancePayment());

        return ContractResponse.from(updated);
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteContract(UUID id) {
        Contract contract = getContractOrThrow(id);
        if (contract.getStatus() == ContractStatus.ACTIVE) {
            throw new InvalidStateTransitionException(
                    "Contract", id, contract.getStatus(), ContractStatus.TERMINATED,
                    contract.getStatus().allowedTransitions()
            );
        }
        contractRepository.delete(contract);
        logger.info("Deleted contract {} for manuscript {}", id, contract.getManuscriptId());
    }
}
