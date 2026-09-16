package com.unibook.publisher.finance.repository;

import com.unibook.publisher.finance.entity.Contract;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class ContractRepository {
    private final Map<UUID, Contract> contracts = new ConcurrentHashMap<>();

    public Contract save(Contract contract) {
        UUID id = contract.id() != null ? contract.id() : UUID.randomUUID();
        Contract toSave = new Contract(
                id,
                contract.manuscriptId(),
                contract.manuscriptTitle(),
                contract.authorId(),
                contract.royaltyPercent(),
                contract.advancePayment(),
                contract.status(),
                contract.authorConfirmedAt(),
                contract.createdAt() != null ? contract.createdAt() : Instant.now()
        );
        contracts.put(id, toSave);
        return toSave;
    }

    public Optional<Contract> findById(UUID id) {
        return Optional.ofNullable(contracts.get(id));
    }

    public Optional<Contract> findByManuscriptId(UUID manuscriptId) {
        return contracts.values().stream()
                .filter(c -> c.manuscriptId().equals(manuscriptId))
                .findFirst();
    }
}
