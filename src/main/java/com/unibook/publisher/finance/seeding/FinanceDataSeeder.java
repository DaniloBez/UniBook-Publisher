package com.unibook.publisher.finance.seeding;

import com.unibook.publisher.common.enums.ContractStatus;
import com.unibook.publisher.common.logging.AppLogger;
import com.unibook.publisher.common.seeding.SeedConstants;
import com.unibook.publisher.finance.repository.ContractRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

@Component
@Profile("!postgres & !test")
@Order(4)
public class FinanceDataSeeder implements ApplicationRunner {
    private final AppLogger logger;
    private final ContractRepository contractRepository;
    private final JdbcTemplate jdbcTemplate;

    public FinanceDataSeeder(AppLogger logger, ContractRepository contractRepository, JdbcTemplate jdbcTemplate) {
        this.logger = logger;
        this.contractRepository = contractRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (contractRepository.count() > 0) {
            logger.info("Contracts already exist in DB. Skipping finance seeding.");
            return;
        }

        logger.info("Seeding finance data...");

        createContract(
                SeedConstants.CONTRACT_DRAFT_ID,
                SeedConstants.MANUSCRIPT_IN_PROGRESS_ID,
                "IN_PROGRESS проєкт",
                SeedConstants.AUTHOR_2_ID,
                new BigDecimal("12.50"),
                new BigDecimal("1000.00"),
                ContractStatus.DRAFT,
                null,
                Instant.now()
        );

        // 2. Активний контракт для опублікованого рукопису
        createContract(
                SeedConstants.CONTRACT_ACTIVE_ID,
                SeedConstants.MANUSCRIPT_PUBLISHED_ID,
                "PUBLISHED проєкт",
                SeedConstants.AUTHOR_1_ID,
                new BigDecimal("15.00"),
                new BigDecimal("2500.00"),
                ContractStatus.ACTIVE,
                Instant.now(),
                Instant.now()
        );

        createContract(
                SeedConstants.CONTRACT_TERMINATED_ID,
                SeedConstants.MANUSCRIPT_REJECTED_ID,
                "TERMINATED проєкт",
                SeedConstants.AUTHOR_2_ID,
                new BigDecimal("10.00"),
                BigDecimal.ZERO,
                ContractStatus.TERMINATED,
                null,
                Instant.now()
        );

        logger.info("Finance data seeding finished.");
    }

    private void createContract(UUID contractId, UUID manuscriptId, String title, UUID authorId, BigDecimal royaltyPercent, BigDecimal advancePayment, ContractStatus status, Instant authorConfirmedAt, Instant createdAt) {
        jdbcTemplate.update(
                "INSERT INTO contracts (contract_id, manuscript_id, title, author_id, royalty_percent, advance_payment, status, author_confirmed_at, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                contractId,
                manuscriptId,
                title,
                authorId,
                royaltyPercent,
                advancePayment,
                status.name(),
                authorConfirmedAt != null ? Timestamp.from(authorConfirmedAt) : null,
                Timestamp.from(createdAt)
        );
    }
}
