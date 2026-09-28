package com.unibook.publisher.communication.seeding;

import com.unibook.publisher.common.logging.AppLogger;
import com.unibook.publisher.communication.repository.NotificationRepository;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Profile("!postgres & !test")
@Order(4)
public class CommunicationDataSeeder implements ApplicationRunner {

    private final AppLogger logger;
    private final NotificationRepository notificationRepository;
    private final JdbcTemplate jdbcTemplate;

    public CommunicationDataSeeder(
            AppLogger logger,
            NotificationRepository notificationRepository,
            JdbcTemplate jdbcTemplate
    ) {
        this.logger = logger;
        this.notificationRepository = notificationRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(@NonNull ApplicationArguments args) {
        if (notificationRepository.count() > 0) {
            logger.info("Notifications already exist. Skipping communication seeding.");
            return;
        }

        logger.info("Seeding communication data...");

        // TODO: додати seed-нотифікації

        logger.info("Communication seeding complete.");
    }
}
