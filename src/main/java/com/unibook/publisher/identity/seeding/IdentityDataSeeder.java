package com.unibook.publisher.identity.seeding;

import com.unibook.publisher.common.enums.UserRole;
import com.unibook.publisher.common.logging.AppLogger;
import com.unibook.publisher.common.seeding.SeedConstants;
import com.unibook.publisher.identity.repository.UserRepository;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@Profile("!postgres & !test")   // Запускаємо якщо не тести або не працюємо з postgreSQL
@Order(1)                       // Виконується першим, оскільки інші об'єкти посилаються на id клієнтів
public class IdentityDataSeeder implements ApplicationRunner {

    // Оскільки User має @GeneratedValue(strategy = GenerationType.UUID),
    // Hibernate забороняє встановлювати кастомний id через persist() або save()
    // (вважає ентіті detached і кидає EntityExistsException).
    // Тому для початкового наповнення з фіксованими UUID використовуємо прямий JdbcTemplate.

    private final AppLogger logger;
    private final UserRepository userRepository;
    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;

    public IdentityDataSeeder(
            AppLogger logger,
            UserRepository userRepository,
            JdbcTemplate jdbcTemplate,
            PasswordEncoder passwordEncoder
    ) {
        this.logger = logger;
        this.userRepository = userRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(@NonNull ApplicationArguments args) {
        if (userRepository.count() > 0) {
            logger.info("Users already exist in DB. Skipping identity seeding.");
            return;
        }

        logger.info("Seeding identity data...");

        String defaultPassword = passwordEncoder.encode(SeedConstants.DEFAULT_PASSWORD);

        createUser(
                SeedConstants.ADMIN_ID,
                "admin@unibook-demo.local",
                defaultPassword,
                UserRole.ADMIN,
                "Модератор",
                "Системний адміністратор платформи UniBook",
                "https://api.dicebear.com/10.x/lorelei/svg?seed=admin",
                "uk-UA"
        );

        createUser(
                SeedConstants.CHIEF_EDITOR_ID,
                "chief.editor@unibook-demo.local",
                defaultPassword,
                UserRole.CHIEF_EDITOR,
                "Головний редактор",
                "Головний редактор видавництва, модерація нових рукописів",
                "https://api.dicebear.com/10.x/lorelei/svg?seed=chief",
                "uk-UA"
        );

        createUser(
                SeedConstants.EDITOR_ID,
                "editor@unibook-demo.local",
                defaultPassword,
                UserRole.EDITOR,
                "Редактор",
                "Провідна літературна редакторка художньої прози",
                "https://api.dicebear.com/10.x/lorelei/svg?seed=editor",
                "uk-UA"
        );

        createUser(
                SeedConstants.DESIGNER_ID,
                "designer@unibook-demo.local",
                defaultPassword,
                UserRole.DESIGNER,
                "Дизайнер",
                "Книжковий ілюстратор та концепт-художник обкладинок",
                "https://api.dicebear.com/10.x/lorelei/svg?seed=designer",
                "uk-UA"
        );

        createUser(
                SeedConstants.AUTHOR_1_ID,
                "author1@unibook-demo.local",
                defaultPassword,
                UserRole.AUTHOR,
                "Перший автор на платформі",
                "Письменник наукової фантастики та космічних пригод",
                "https://api.dicebear.com/10.x/lorelei/svg?seed=author1",
                "uk-UA"
        );

        createUser(
                SeedConstants.AUTHOR_2_ID,
                "author2@unibook-demo.local",
                defaultPassword,
                UserRole.AUTHOR,
                "Second author on the platform",
                "An author of urban and dark fantasy",
                "https://api.dicebear.com/10.x/lorelei/svg?seed=author2",
                "en-US"
        );

        createUser(
                SeedConstants.ACCOUNTANT_ID,
                "accountant@unibook-demo.local",
                defaultPassword,
                UserRole.ACCOUNTANT,
                "Бухгалтер",
                "",
                "https://api.dicebear.com/10.x/lorelei/svg?seed=accountant",
                "uk-UA"
        );

        logger.info("Identity seeding complete.");
    }

    private void createUser(UUID id, String email, String pass, UserRole role,
                            String name, String bio, String avatar, String locale) {
        jdbcTemplate.update(
                "INSERT INTO users (id, email, hashed_password, role) VALUES (?, ?, ?, ?)",
                id, email, pass, role.name()
        );

        UUID profileId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO user_profiles (id, user_id, display_name, bio, avatar_url, preferred_locale) VALUES (?, ?, ?, ?, ?, ?)",
                profileId, id, name, bio, avatar, locale
        );
    }
}
