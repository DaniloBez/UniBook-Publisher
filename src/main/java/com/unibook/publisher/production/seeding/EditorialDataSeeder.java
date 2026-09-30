package com.unibook.publisher.production.seeding;

import com.unibook.publisher.common.logging.AppLogger;
import com.unibook.publisher.common.seeding.SeedConstants;
import com.unibook.publisher.production.enums.SuggestionStatus;
import com.unibook.publisher.production.enums.ThreadStatus;
import com.unibook.publisher.production.repository.ChapterRepository;
import com.unibook.publisher.production.repository.CoverVersionRepository;
import com.unibook.publisher.production.repository.FeedbackThreadRepository;
import com.unibook.publisher.production.repository.RevisionRepository;
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
@Order(3) // Виконується після ProductionDataSeeder, оскільки розділи та обкладинки посилаються на рукописи
public class EditorialDataSeeder implements ApplicationRunner {

    // Як і в інших сидерах: id в ентіті генеруються (GenerationType.UUID), тому для
    // фіксованих UUID з SeedConstants використовуємо JdbcTemplate. Для перевірки наявності даних - репозиторії.

    private final AppLogger logger;
    private final ChapterRepository chapterRepository;
    private final RevisionRepository revisionRepository;
    private final FeedbackThreadRepository threadRepository;
    private final CoverVersionRepository coverVersionRepository;
    private final JdbcTemplate jdbcTemplate;

    public EditorialDataSeeder(
            AppLogger logger,
            ChapterRepository chapterRepository,
            RevisionRepository revisionRepository,
            FeedbackThreadRepository threadRepository,
            CoverVersionRepository coverVersionRepository,
            JdbcTemplate jdbcTemplate
    ) {
        this.logger = logger;
        this.chapterRepository = chapterRepository;
        this.revisionRepository = revisionRepository;
        this.threadRepository = threadRepository;
        this.coverVersionRepository = coverVersionRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public void run(@NonNull ApplicationArguments args) {
        if (chapterRepository.count() > 0
                || revisionRepository.count() > 0
                || threadRepository.count() > 0
                || coverVersionRepository.count() > 0) {
            logger.info("Editorial data already exists in DB. Skipping editorial seeding.");
            return;
        }

        logger.info("Seeding editorial data...");

        seedChapters();
        seedRevisions();
        seedThreads();
        seedCovers();

        logger.info("Editorial data seeding finished.");
    }

    private void seedChapters() {
        createChapter(SeedConstants.CHAPTER_IN_PROGRESS_1_ID, SeedConstants.MANUSCRIPT_IN_PROGRESS_ID, "Пролог", 1);
        createChapter(SeedConstants.CHAPTER_IN_PROGRESS_2_ID, SeedConstants.MANUSCRIPT_IN_PROGRESS_ID, "Перша зустріч", 2);
        createChapter(SeedConstants.CHAPTER_IN_PROGRESS_3_ID, SeedConstants.MANUSCRIPT_IN_PROGRESS_ID, "Втеча", 3);

        createChapter(SeedConstants.CHAPTER_TEXT_APPROVED_1_ID, SeedConstants.MANUSCRIPT_TEXT_APPROVED_ID, "Початок подорожі", 1);
        createChapter(SeedConstants.CHAPTER_TEXT_APPROVED_2_ID, SeedConstants.MANUSCRIPT_TEXT_APPROVED_ID, "Перевал", 2);
        createChapter(SeedConstants.CHAPTER_TEXT_APPROVED_3_ID, SeedConstants.MANUSCRIPT_TEXT_APPROVED_ID, "Повернення", 3);

        createChapter(SeedConstants.CHAPTER_IN_DESIGN_1_ID, SeedConstants.MANUSCRIPT_IN_DESIGN_ID, "Світанок", 1);
        createChapter(SeedConstants.CHAPTER_IN_DESIGN_2_ID, SeedConstants.MANUSCRIPT_IN_DESIGN_ID, "Місто на межі", 2);
        createChapter(SeedConstants.CHAPTER_IN_DESIGN_3_ID, SeedConstants.MANUSCRIPT_IN_DESIGN_ID, "Остання вежа", 3);

        createChapter(SeedConstants.CHAPTER_PUBLISHED_1_ID, SeedConstants.MANUSCRIPT_PUBLISHED_ID, "Справа починається", 1);
        createChapter(SeedConstants.CHAPTER_PUBLISHED_2_ID, SeedConstants.MANUSCRIPT_PUBLISHED_ID, "Підозрювані", 2);
        createChapter(SeedConstants.CHAPTER_PUBLISHED_3_ID, SeedConstants.MANUSCRIPT_PUBLISHED_ID, "Розв'язка", 3);
    }

    private void seedRevisions() {
        Instant now = Instant.now();

        // Розділ 1 має 3 версії: базова та дві з правками редактора
        createRevision(SeedConstants.REVISION_IN_PROGRESS_1_V1_ID, SeedConstants.CHAPTER_IN_PROGRESS_1_ID, 1,
                "https://localhost:8080/revisions/in-progress-ch1-v1.txt", SeedConstants.AUTHOR_2_ID, now.minus(5, ChronoUnit.DAYS),
                PROLOGUE_V1);
        createRevision(SeedConstants.REVISION_IN_PROGRESS_1_V2_ID, SeedConstants.CHAPTER_IN_PROGRESS_1_ID, 2,
                "https://localhost:8080/revisions/in-progress-ch1-v2.txt", SeedConstants.EDITOR_ID, now.minus(3, ChronoUnit.DAYS),
                PROLOGUE_V2);
        createRevision(SeedConstants.REVISION_IN_PROGRESS_1_V3_ID, SeedConstants.CHAPTER_IN_PROGRESS_1_ID, 3,
                "https://localhost:8080/revisions/in-progress-ch1-v3.txt", SeedConstants.AUTHOR_2_ID, now.minus(1, ChronoUnit.DAYS),
                PROLOGUE_V3);
        createRevision(SeedConstants.REVISION_IN_PROGRESS_2_V1_ID, SeedConstants.CHAPTER_IN_PROGRESS_2_ID, 1,
                "https://localhost:8080/revisions/in-progress-ch2-v1.txt", SeedConstants.AUTHOR_2_ID, now.minus(2, ChronoUnit.DAYS),
                "Вона побачила його вперше біля старого мосту. Він тримав у руках потертий компас.");
    }

    private void seedThreads() {
        Instant now = Instant.now();

        // Пропозиція правки з цитатою з ревізії v3 (позиції обчислюються з тексту, щоб цитата була валідною)
        String quote = "тихий вітер";
        int from = PROLOGUE_V3.indexOf(quote);
        createThread(SeedConstants.THREAD_SUGGESTION_OPEN_ID, SeedConstants.CHAPTER_IN_PROGRESS_1_ID, SeedConstants.EDITOR_ID,
                ThreadStatus.OPEN, true, "легкий вітер", SuggestionStatus.PENDING, now.minus(20, ChronoUnit.HOURS),
                SeedConstants.REVISION_IN_PROGRESS_1_V3_ID, quote, from, from + quote.length());
        createMessage(SeedConstants.MESSAGE_SUGGESTION_1_ID, SeedConstants.THREAD_SUGGESTION_OPEN_ID, SeedConstants.EDITOR_ID,
                "Пропоную замінити епітет - так звучить природніше.", now.minus(20, ChronoUnit.HOURS));

        // Обговорення, яке вже закрите
        createThread(SeedConstants.THREAD_DISCUSSION_RESOLVED_ID, SeedConstants.CHAPTER_IN_PROGRESS_2_ID, SeedConstants.AUTHOR_2_ID,
                ThreadStatus.RESOLVED, false, null, null, now.minus(30, ChronoUnit.HOURS),
                null, null, null, null);
        createMessage(SeedConstants.MESSAGE_DISCUSSION_1_ID, SeedConstants.THREAD_DISCUSSION_RESOLVED_ID, SeedConstants.AUTHOR_2_ID,
                "Чи потрібна тут сцена біля мосту, чи краще скоротити?", now.minus(30, ChronoUnit.HOURS));
        createMessage(SeedConstants.MESSAGE_DISCUSSION_2_ID, SeedConstants.THREAD_DISCUSSION_RESOLVED_ID, SeedConstants.EDITOR_ID,
                "Залишаємо, вона потрібна для розвитку героїв.", now.minus(29, ChronoUnit.HOURS));
    }

    private void seedCovers() {
        Instant now = Instant.now();
        createCover(SeedConstants.COVER_IN_DESIGN_V1_ID, SeedConstants.MANUSCRIPT_IN_DESIGN_ID,
                "https://localhost:8080/covers/in-design-v1.png", SeedConstants.DESIGNER_ID, 1, now.minus(2, ChronoUnit.DAYS));
        createCover(SeedConstants.COVER_PUBLISHED_V1_ID, SeedConstants.MANUSCRIPT_PUBLISHED_ID,
                "https://localhost:8080/covers/published-v1.png", SeedConstants.DESIGNER_ID, 1, now.minus(10, ChronoUnit.DAYS));
    }

    private void createChapter(UUID id, UUID manuscriptId, String title, int index) {
        jdbcTemplate.update(
                "INSERT INTO chapters (chapter_id, manuscript_id, chapter_title, chapter_index) VALUES (?, ?, ?, ?)",
                id, manuscriptId, title, index
        );
    }

    private void createRevision(UUID id, UUID chapterId, int versionNumber, String fileUrl, UUID uploadedBy, Instant uploadedAt, String text) {
        jdbcTemplate.update(
                "INSERT INTO revisions (revision_id, chapter_id, version_number, file_url, uploaded_by_user_id, uploaded_at, text_content) VALUES (?, ?, ?, ?, ?, ?, ?)",
                id, chapterId, versionNumber, fileUrl, uploadedBy, Timestamp.from(uploadedAt), text
        );
    }

    private void createThread(UUID id, UUID chapterId, UUID createdBy, ThreadStatus status, boolean isSuggestion, String suggestedText,
                              SuggestionStatus suggestionStatus, Instant createdAt, UUID targetRevisionId, String quotedText,
                              Integer positionFrom, Integer positionTo) {
        jdbcTemplate.update(
                "INSERT INTO feedback_threads (id, chapter_id, created_by_user_id, status, is_suggestion, suggested_text, suggestion_status, created_at, target_revision_id, quoted_text, position_from, position_to) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                id, chapterId, createdBy, status.name(), isSuggestion, suggestedText,
                suggestionStatus == null ? null : suggestionStatus.name(), Timestamp.from(createdAt),
                targetRevisionId, quotedText, positionFrom, positionTo
        );
    }

    private void createMessage(UUID id, UUID threadId, UUID senderId, String content, Instant sentAt) {
        jdbcTemplate.update(
                "INSERT INTO thread_messages (id, thread_id, sender_user_id, content, sent_at) VALUES (?, ?, ?, ?, ?)",
                id, threadId, senderId, content, Timestamp.from(sentAt)
        );
    }

    private void createCover(UUID id, UUID manuscriptId, String fileUrl, UUID uploadedBy, int versionNumber, Instant uploadedAt) {
        jdbcTemplate.update(
                "INSERT INTO cover_versions (id, manuscript_id, file_url, uploaded_by_user_id, version_number, uploaded_at) VALUES (?, ?, ?, ?, ?, ?)",
                id, manuscriptId, fileUrl, uploadedBy, versionNumber, Timestamp.from(uploadedAt)
        );
    }

    private static final String PROLOGUE_V1 = """
            Ніч була темна. Над полем віяв тихий вітер, і десь далеко гавкав собака.
            Вона йшла додому і не знала, що скоро все зміниться.
            """;

    private static final String PROLOGUE_V2 = """
            Ніч була темною. Над полем віяв тихий вітер, і десь далеко гавкав собака.
            Вона поверталася додому і не здогадувалася, що скоро все зміниться.
            """;

    private static final String PROLOGUE_V3 = """
            Ніч була темною та безмісячною. Над полем віяв тихий вітер, і десь далеко гавкав собака.
            Вона поверталася додому і не здогадувалася, що скоро все зміниться назавжди.
            """;
}
