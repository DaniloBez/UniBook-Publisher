package com.unibook.publisher.communication.seeding;

import com.unibook.publisher.common.logging.AppLogger;
import com.unibook.publisher.common.seeding.SeedConstants;
import com.unibook.publisher.communication.entity.NotificationType;
import com.unibook.publisher.communication.repository.NotificationRepository;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Component
@Profile("!postgres & !test")
@Order(5)
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
    @Transactional
    public void run(@NonNull ApplicationArguments args) {
        if (notificationRepository.count() > 0) {
            logger.info("Notifications already exist. Skipping communication seeding.");
            return;
        }

        logger.info("Seeding communication data...");

        Instant now = Instant.now();

        createNotification(
                SeedConstants.AUTHOR_1_ID,
                null,
                SeedConstants.MANUSCRIPT_SUBMITTED_ID,
                "Рукопис подано",
                "Ваш рукопис 'SUBMITTED проєкт' успішно надіслано та передано на модерацію.",
                NotificationType.MANUSCRIPT_STATUS,
                true,
                now.minus(10, ChronoUnit.DAYS)
        );

        createNotification(
                SeedConstants.EDITOR_ID,
                SeedConstants.CHIEF_EDITOR_ID,
                SeedConstants.MANUSCRIPT_SUBMITTED_ID,
                "Нове призначення",
                "Вас призначено як EDITOR для книги 'SUBMITTED проєкт'.",
                NotificationType.ASSIGNMENT,
                false,
                now.minus(9, ChronoUnit.DAYS)
        );

        createNotification(
                SeedConstants.AUTHOR_1_ID,
                SeedConstants.CHIEF_EDITOR_ID,
                SeedConstants.MANUSCRIPT_SUBMITTED_ID,
                "Оновлення команди проєкту",
                "До роботи над вашою книгою 'SUBMITTED проєкт' призначено EDITOR.",
                NotificationType.MANUSCRIPT_STATUS,
                true,
                now.minus(9, ChronoUnit.DAYS)
        );

        createNotification(
                SeedConstants.AUTHOR_2_ID,
                SeedConstants.EDITOR_ID,
                SeedConstants.MANUSCRIPT_IN_PROGRESS_ID,
                "Оновлення розділу книги",
                "До розділу рукопису 'IN_PROGRESS проєкт' завантажено нову редаговану версію.",
                NotificationType.REVIEW_FEEDBACK,
                true,
                now.minus(3, ChronoUnit.DAYS)
        );

        createNotification(
                SeedConstants.AUTHOR_2_ID,
                SeedConstants.EDITOR_ID,
                SeedConstants.MANUSCRIPT_IN_PROGRESS_ID,
                "Нове обговорення",
                "Відкрито обговорення розділу в книзі 'IN_PROGRESS проєкт': легкий вітер",
                NotificationType.REVIEW_FEEDBACK,
                false,
                now.minus(20, ChronoUnit.HOURS)
        );

        createNotification(
                SeedConstants.AUTHOR_2_ID,
                SeedConstants.EDITOR_ID,
                SeedConstants.MANUSCRIPT_IN_PROGRESS_ID,
                "Нове повідомлення в обговоренні 'легкий вітер'.",
                "Пропоную замінити епітет - так звучить природніше.",
                NotificationType.REVIEW_FEEDBACK,
                false,
                now.minus(20, ChronoUnit.HOURS)
        );

        createNotification(
                SeedConstants.AUTHOR_1_ID,
                SeedConstants.EDITOR_ID,
                SeedConstants.MANUSCRIPT_TEXT_APPROVED_ID,
                "Редагування тексту завершено",
                "Роботу редактора над текстом книги 'TEXT_APPROVED проєкт' офіційно завершено.",
                NotificationType.MANUSCRIPT_STATUS,
                true,
                now.minus(4, ChronoUnit.DAYS)
        );

        createNotification(
                SeedConstants.DESIGNER_ID,
                SeedConstants.CHIEF_EDITOR_ID,
                SeedConstants.MANUSCRIPT_IN_DESIGN_ID,
                "Нове призначення",
                "Вас призначено як DESIGNER для книги 'IN_DESIGN проєкт'.",
                NotificationType.ASSIGNMENT,
                true,
                now.minus(5, ChronoUnit.DAYS)
        );

        createNotification(
                SeedConstants.AUTHOR_2_ID,
                SeedConstants.DESIGNER_ID,
                SeedConstants.MANUSCRIPT_IN_DESIGN_ID,
                "Нова версія обкладинки",
                "Дизайнер завантажив новий варіант обкладинки для книги 'IN_DESIGN проєкт'.",
                NotificationType.MANUSCRIPT_STATUS,
                false,
                now.minus(2, ChronoUnit.DAYS)
        );

        createNotification(
                SeedConstants.AUTHOR_1_ID,
                null,
                SeedConstants.MANUSCRIPT_PUBLISHED_ID,
                "Контракт підтверджено",
                "Контракт на публікацію книги 'PUBLISHED проєкт' успішно укладено та підтверджено.",
                NotificationType.SYSTEM,
                true,
                now.minus(15, ChronoUnit.DAYS)
        );

        createNotification(
                SeedConstants.AUTHOR_1_ID,
                null,
                SeedConstants.MANUSCRIPT_PUBLISHED_ID,
                "Оновлення відсотка роялті",
                "Ставку роялті для книги 'PUBLISHED проєкт' змінено на 15.00%.",
                NotificationType.SYSTEM,
                true,
                now.minus(12, ChronoUnit.DAYS)
        );

        createNotification(
                SeedConstants.AUTHOR_1_ID,
                null,
                SeedConstants.MANUSCRIPT_PUBLISHED_ID,
                "Книгу опубліковано!",
                "Вітаємо! Книгу 'PUBLISHED проєкт' успішно опубліковано у видавництві UniBook.",
                NotificationType.MANUSCRIPT_STATUS,
                true,
                now.minus(10, ChronoUnit.DAYS)
        );

        createNotification(
                SeedConstants.AUTHOR_1_ID,
                SeedConstants.CHIEF_EDITOR_ID,
                SeedConstants.MANUSCRIPT_POSTPONED_ID,
                "Розгляд рукопису відкладено",
                "Розгляд рукопису 'POSTPONED проєкт' відкладено. Зауваження: Необхідно доопрацювати третій розділ.",
                NotificationType.MANUSCRIPT_STATUS,
                false,
                now.minus(1, ChronoUnit.DAYS)
        );

        createNotification(
                SeedConstants.AUTHOR_2_ID,
                SeedConstants.CHIEF_EDITOR_ID,
                SeedConstants.MANUSCRIPT_REJECTED_ID,
                "Рукопис відхилено",
                "Рукопис 'REJECTED проєкт' було відхилено. Причина: Не відповідає редакційній політиці.",
                NotificationType.MANUSCRIPT_STATUS,
                true,
                now.minus(12, ChronoUnit.DAYS)
        );

        logger.info("Communication seeding complete.");
    }

    private void createNotification(
            UUID recipientId,
            UUID senderId,
            UUID targetId,
            String title,
            String message,
            NotificationType type,
            boolean isRead,
            Instant createdAt
    ) {
        jdbcTemplate.update(
                "INSERT INTO notifications (id, recipient_id, sender_id, target_id, title, message, type, is_read, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                UUID.randomUUID(),
                recipientId,
                senderId,
                targetId,
                title,
                message,
                type.name(),
                isRead,
                Timestamp.from(createdAt)
        );
    }
}
