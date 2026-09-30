package com.unibook.publisher.production.seeding;

import com.unibook.publisher.common.enums.UserRole;
import com.unibook.publisher.common.logging.AppLogger;
import com.unibook.publisher.common.seeding.SeedConstants;
import com.unibook.publisher.production.enums.ManuscriptStatus;
import com.unibook.publisher.production.repository.GenreRepository;
import com.unibook.publisher.production.repository.ManuscriptRepository;
import com.unibook.publisher.production.repository.TeamAssignmentRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Component
@Profile("!postgres & !test")
@Order(2)
public class ProductionDataSeeder implements ApplicationRunner {
    private final AppLogger logger;
    private final GenreRepository genreRepository;
    private final ManuscriptRepository manuscriptRepository;
    private final TeamAssignmentRepository teamAssignmentRepository;
    private final JdbcTemplate jdbcTemplate;

    public ProductionDataSeeder(AppLogger logger, GenreRepository genreRepository, ManuscriptRepository manuscriptRepository, TeamAssignmentRepository teamAssignmentRepository, JdbcTemplate jdbcTemplate) {
        this.logger = logger;
        this.genreRepository = genreRepository;
        this.manuscriptRepository = manuscriptRepository;
        this.teamAssignmentRepository = teamAssignmentRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (genreRepository.count() > 0
                || manuscriptRepository.count() > 0
                || teamAssignmentRepository.count() > 0) {
            logger.info("Production data already exists in DB. Skipping production seeding.");
            return;
        }

        logger.info("Seeding production data...");

        //genre
        createGenre(SeedConstants.GENRE_FANTASY_ID, "Фантастика");
        createGenre(SeedConstants.GENRE_MYSTERY_ID, "Детектив");
        createGenre(SeedConstants.GENRE_NONFICTION_ID, "Нон-фікшн");
        createGenre(SeedConstants.GENRE_FICTION_ID, "Художня література");
        createGenre(SeedConstants.GENRE_THRILLER_ID, "Трилер");
        createGenre(SeedConstants.GENRE_ROMANCE_ID, "Любовний роман");
        createGenre(SeedConstants.GENRE_HISTORICAL_FICTION_ID, "Історичний роман");

        //manuscript
        createManuscript(
                SeedConstants.MANUSCRIPT_SUBMITTED_ID,
                "SUBMITTED проєкт",
                SeedConstants.AUTHOR_1_ID,
                ManuscriptStatus.SUBMITTED,
                Set.of(SeedConstants.GENRE_FANTASY_ID, SeedConstants.GENRE_FICTION_ID),
                "SUBMITTED проєкт анотація",
                "https://localhost:8080/drafts/submitted.pdf",
                Instant.now()
        );

        createManuscript(
                SeedConstants.MANUSCRIPT_IN_PROGRESS_ID,
                "IN_PROGRESS проєкт",
                SeedConstants.AUTHOR_2_ID,
                ManuscriptStatus.IN_PROGRESS,
                Set.of(SeedConstants.GENRE_FANTASY_ID, SeedConstants.GENRE_THRILLER_ID),
                "IN_PROGRESS проєкт анотація",
                "https://localhost:8080/drafts/in-progress.pdf",
                Instant.now()
        );

        createManuscript(
                SeedConstants.MANUSCRIPT_TEXT_APPROVED_ID,
                "TEXT_APPROVED проєкт",
                SeedConstants.AUTHOR_1_ID,
                ManuscriptStatus.TEXT_APPROVED,
                Set.of(SeedConstants.GENRE_FANTASY_ID, SeedConstants.GENRE_FICTION_ID),
                "TEXT_APPROVED проєкт анотація",
                "https://localhost:8080/drafts/text-approved.pdf",
                Instant.now()
        );

        createManuscript(
                SeedConstants.MANUSCRIPT_IN_DESIGN_ID,
                "IN_DESIGN проєкт",
                SeedConstants.AUTHOR_2_ID,
                ManuscriptStatus.IN_DESIGN,
                Set.of(SeedConstants.GENRE_FANTASY_ID),
                "IN_DESIGN проєкт анотація",
                "https://localhost:8080/drafts/in-design.pdf",
                Instant.now()
        );

        createManuscript(
                SeedConstants.MANUSCRIPT_REJECTED_ID,
                "REJECTED проєкт",
                SeedConstants.AUTHOR_2_ID,
                ManuscriptStatus.REJECTED,
                Set.of(SeedConstants.GENRE_FICTION_ID),
                "REJECTED проєкт анотація",
                "https://localhost:8080/drafts/rejected.pdf",
                Instant.now()
        );

        createManuscript(
                SeedConstants.MANUSCRIPT_PUBLISHED_ID,
                "PUBLISHED проєкт",
                SeedConstants.AUTHOR_1_ID,
                ManuscriptStatus.PUBLISHED,
                Set.of(SeedConstants.GENRE_MYSTERY_ID, SeedConstants.GENRE_THRILLER_ID),
                "PUBLISHED проєкт анотація",
                "https://localhost:8080/drafts/published.pdf",
                Instant.now()
        );

        createManuscript(
                SeedConstants.MANUSCRIPT_POSTPONED_ID,
                "POSTPONED проєкт",
                SeedConstants.AUTHOR_1_ID,
                ManuscriptStatus.POSTPONED,
                Set.of(SeedConstants.GENRE_HISTORICAL_FICTION_ID),
                "PUBLISHED проєкт анотація",
                "https://localhost:8080/drafts/postponed.pdf",
                Instant.now()
        );

        //team assignment
        createTeamAssignment(
                SeedConstants.ASSIGNMENT_EDITOR_SUBMITTED_ID,
                SeedConstants.MANUSCRIPT_SUBMITTED_ID,
                SeedConstants.EDITOR_ID,
                UserRole.EDITOR,
                Instant.now()
        );

        createTeamAssignment(
                SeedConstants.ASSIGNMENT_DESIGNER_IN_DESIGN_ID,
                SeedConstants.MANUSCRIPT_IN_DESIGN_ID,
                SeedConstants.DESIGNER_ID,
                UserRole.DESIGNER,
                Instant.now()
        );

        logger.info("Production data seeding finished.");
    }

    private void createGenre(UUID genreId, String genreName) {
        jdbcTemplate.update(
                "INSERT INTO genres (genre_id, genre_name) VALUES (?, ?)",
                genreId, genreName
        );
    }

    private void createManuscript(UUID manuscriptId, String title, UUID authorId, ManuscriptStatus status, Set<UUID> genreIds, String annotation, String draftFileUrl, Instant submittedAt) {
        jdbcTemplate.update(
                "INSERT INTO manuscripts (manuscript_id, title, author_id, status, annotation, draft_file_url, submitted_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
                manuscriptId, title, authorId, status.name(), annotation, draftFileUrl, Timestamp.from(submittedAt)
        );

        if (genreIds != null && !genreIds.isEmpty()) {
            for (UUID genreId : genreIds) {
                jdbcTemplate.update(
                        "INSERT INTO manuscript_genres (manuscript_id, genre_id) VALUES (?, ?)",
                        manuscriptId, genreId
                );
            }
        }
    }

    private void createTeamAssignment(UUID assignmentId, UUID manuscriptId, UUID userId, UserRole role, Instant assignedAt) {
        jdbcTemplate.update(
                "INSERT INTO team_assignments (assignment_id, manuscript_id, user_id, role, assigned_at) VALUES (?, ?, ?, ?, ?)",
                assignmentId, manuscriptId, userId, role.name(), Timestamp.from(assignedAt)
        );
    }
}
