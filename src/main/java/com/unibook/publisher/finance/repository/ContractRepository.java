package com.unibook.publisher.finance.repository;

import com.unibook.publisher.finance.entity.Contract;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ContractRepository extends JpaRepository<Contract, UUID> {
    Optional<Contract> findByManuscript_ManuscriptId(UUID manuscriptId);
}
