package com.unibook.publisher.production.service;

import com.unibook.publisher.common.enums.UserRole;
import com.unibook.publisher.common.event.RevisionAddedEvent;
import com.unibook.publisher.common.exception.badrequest.InvalidFileTypeException;
import com.unibook.publisher.common.exception.business.BusinessRuleViolationException;
import com.unibook.publisher.common.exception.business.FileUploadException;
import com.unibook.publisher.common.exception.notfound.ChapterNotFoundException;
import com.unibook.publisher.common.exception.notfound.ManuscriptNotFoundException;
import com.unibook.publisher.common.exception.notfound.RevisionNotFoundException;
import com.unibook.publisher.common.exception.security.ForbiddenActionException;
import com.unibook.publisher.common.exception.state.InvalidStateTransitionException;
import com.unibook.publisher.common.exception.storage.FileReadException;
import com.unibook.publisher.common.logging.AppLogger;
import com.unibook.publisher.production.entity.Chapter;
import com.unibook.publisher.production.entity.Manuscript;
import com.unibook.publisher.production.entity.request.DiffRequest;
import com.unibook.publisher.production.entity.response.DiffResponse;
import com.unibook.publisher.production.enums.ManuscriptStatus;
import com.unibook.publisher.production.entity.Revision;
import com.unibook.publisher.production.entity.request.ChapterCreationRequest;
import com.unibook.publisher.production.entity.request.ChapterUpdateRequest;
import com.unibook.publisher.production.entity.response.ChapterResponse;
import com.unibook.publisher.production.entity.response.RevisionResponse;
import com.unibook.publisher.production.repository.ChapterRepository;
import com.unibook.publisher.production.repository.ManuscriptRepository;
import com.unibook.publisher.production.repository.RevisionRepository;
import com.unibook.publisher.production.repository.TeamAssignmentRepository;
import com.unibook.publisher.storage.FileStorageService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ChapterServiceImpl implements ChapterService {
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".txt", ".md");
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "text/plain",
            "text/markdown",
            "text/x-markdown",
            "application/octet-stream"
    );

    private final ChapterRepository chapterRepository;
    private final ManuscriptRepository manuscriptRepository;
    private final RevisionRepository revisionRepository;
    private final TeamAssignmentRepository teamAssignmentRepository;
    private final DiffService diffService;
    private final FileStorageService fileStorageService;
    private final ApplicationEventPublisher publisher;
    private final AppLogger logger;

    public ChapterServiceImpl(ChapterRepository chapterRepository, ManuscriptRepository manuscriptRepository, RevisionRepository revisionRepository, TeamAssignmentRepository teamAssignmentRepository, DiffService diffService, FileStorageService fileStorageService, ApplicationEventPublisher publisher, AppLogger logger) {
        this.chapterRepository = chapterRepository;
        this.manuscriptRepository = manuscriptRepository;
        this.revisionRepository = revisionRepository;
        this.teamAssignmentRepository = teamAssignmentRepository;
        this.diffService = diffService;
        this.fileStorageService = fileStorageService;
        this.publisher = publisher;
        this.logger = logger;
    }

    @Override
    @Transactional
    public ChapterResponse createChapter(UUID manuscriptId, UUID authorId, ChapterCreationRequest request) {
        Manuscript manuscript = manuscriptRepository.findById(manuscriptId)
                .orElseThrow(() -> new ManuscriptNotFoundException(manuscriptId));
        if(!manuscript.getAuthorId().equals(authorId)) {
            throw new ForbiddenActionException("Автор не має прав на додавання розділів до цього рукопису");
        }
        if(manuscript.getStatus() != ManuscriptStatus.IN_PROGRESS) {
            throw new InvalidStateTransitionException(
                    "Manuscript",
                    manuscriptId,
                    manuscript.getStatus(),
                    ManuscriptStatus.IN_PROGRESS,
                    manuscript.getStatus().allowedTransitions()
            );
        }
        Chapter chapter = new Chapter(manuscript, request.chapterTitle(), request.chapterIndex());
        Chapter saved = chapterRepository.save(chapter);

        logger.info(
            "Created chapter {} '{}' for manuscript {} by user {}",
            saved.getChapterId(),
            saved.getChapterTitle(),
            manuscriptId,
            authorId
        );

        return ChapterResponse.from(saved);
    }

    @Override
    public List<ChapterResponse> getChaptersByManuscriptId(UUID manuscriptId) {
        if(manuscriptRepository.findById(manuscriptId).isEmpty()) {
            throw new ManuscriptNotFoundException(manuscriptId);
        }
        return chapterRepository.findByManuscript_ManuscriptIdOrderByChapterIndex(manuscriptId).stream()
                .map(ChapterResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public RevisionResponse uploadRevision(
            UUID chapterId,
            UUID userId,
            InputStream fileStream,
            long size,
            String contentType,
            String originalFilename
    ) {
        validateTextFile(contentType, originalFilename);

        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new ChapterNotFoundException(chapterId));

        Manuscript manuscript = manuscriptRepository.findById(chapter.getManuscriptId())
                .orElseThrow(() -> new ManuscriptNotFoundException(chapter.getManuscriptId()));

        if(manuscript.getStatus() != ManuscriptStatus.IN_PROGRESS) {
            throw new InvalidStateTransitionException(
                    "Manuscript",
                    manuscript.getManuscriptId(),
                    manuscript.getStatus(),
                    ManuscriptStatus.IN_PROGRESS,
                    manuscript.getStatus().allowedTransitions()
            );
        }

        boolean isAuthor = manuscript.getAuthorId().equals(userId);
        boolean isEditor = teamAssignmentRepository.existsByManuscript_ManuscriptIdAndUserIdAndRole(manuscript.getManuscriptId(), userId, UserRole.EDITOR);

        if(!isAuthor && !isEditor)
            throw new ForbiddenActionException("Користувач не має прав на завантаження ревізій для цього розділу");

        String extension = extractExtension(originalFilename);
        String uniqueFileName = UUID.randomUUID() + extension;
        String s3Path = String.format("%s/%s/chapters/%s/revisions/%s",
                manuscript.getAuthorId(),
                manuscript.getManuscriptId(),
                chapterId,
                uniqueFileName
        );

        String uploadedPath = fileStorageService.put(s3Path, fileStream, size, contentType);

        try {
            int versionNumber = revisionRepository.findTopByChapter_ChapterIdOrderByVersionNumberDesc(chapterId)
                    .map(revision -> revision.getVersionNumber() + 1)
                    .orElse(1);

            Revision revision = new Revision(versionNumber, uploadedPath, userId, Instant.now());
            chapter.addRevision(revision);
            Revision saved = revisionRepository.save(revision);

            logger.info(
                    "Created revision {} version {} for chapter {} by user {}",
                    saved.getRevisionId(),
                    saved.getVersionNumber(),
                    chapterId,
                    userId
            );

            publisher.publishEvent(new RevisionAddedEvent(
                    manuscript.getManuscriptId(),
                    manuscript.getTitle(),
                    chapter.getChapterId(),
                    saved.getRevisionId(),
                    userId,
                    manuscript.getAuthorId()
            ));

            return RevisionResponse.from(saved);
        } catch (Exception e) {
            try {
                fileStorageService.delete(uploadedPath);
            } catch (Exception cleanupException) {
                logger.warn("Не вдалося видалити файл {} з MinIO під час відкату", uploadedPath, cleanupException);
            }

            throw new FileUploadException(
                    "Не вдалося зберегти версію ревізії для глави " + chapterId, e
            );
        }
    }

    private void validateTextFile(String contentType, String originalFilename) {
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase()))
            throw new InvalidFileTypeException("Некоректний Content-Type: " + contentType);

        if (originalFilename == null)
            throw new InvalidFileTypeException("Назва файлу не може бути порожньою");

        String extension = extractExtension(originalFilename);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new InvalidFileTypeException("Підтримуються лише текстові файли формату .txt або .md");
        }
    }

    private String extractExtension(String filename) {
        int lastIndexOfDot = filename.lastIndexOf(".");
        if (lastIndexOfDot == -1 || lastIndexOfDot == filename.length() - 1)
            throw new InvalidFileTypeException("Файл не містить валідного розширення");

        return filename.substring(lastIndexOfDot).toLowerCase();
    }

    @Override
    public List<RevisionResponse> getRevisionsByChapterId(UUID chapterId) {
        if(chapterRepository.findById(chapterId).isEmpty()) {
            throw new ChapterNotFoundException(chapterId);
        }
        return revisionRepository.findAllByChapter_ChapterIdOrderByVersionNumberAsc(chapterId).stream()
                .map(RevisionResponse::from)
                .toList();
    }

    @Override
    public DiffResponse getDiffChapter(UUID chapterId, DiffRequest request) {
        if (chapterRepository.findById(chapterId).isEmpty())
            throw new ChapterNotFoundException(chapterId);

        Revision fromRevision = revisionRepository.findById(request.fromRevisionId())
                .orElseThrow(() -> new RevisionNotFoundException(request.fromRevisionId()));

        Revision toRevision = revisionRepository.findById(request.toRevisionId())
                .orElseThrow(() -> new RevisionNotFoundException(request.toRevisionId()));

        if (!fromRevision.getChapterId().equals(chapterId) || !toRevision.getChapterId().equals(chapterId))
            throw new BusinessRuleViolationException("Запитані ревізії не належать розділу з ID: " + chapterId);

        List<String> oldLines = readRevisionLines(fromRevision.getFileUrl());
        List<String> newLines = readRevisionLines(toRevision.getFileUrl());

        return diffService.compare(
                request.fromRevisionId(),
                request.toRevisionId(),
                oldLines,
                newLines
        );
    }

    private List<String> readRevisionLines(String fileUrl) {
        try (InputStream inputStream = fileStorageService.get(fileUrl);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {

            return reader.lines().toList();
        } catch (UncheckedIOException | IOException e) {
            logger.error("Не вдалося прочитати файл ревізії зі сховища: {}", fileUrl, e);
            throw new FileReadException(fileUrl);
        }
    }

    @Override
    @Transactional
    public ChapterResponse updateChapter(UUID chapterId, UUID authorId, ChapterUpdateRequest request) {
        Chapter chapter = findChapterOwnedBy(chapterId, authorId, "редагування");
        chapter.setChapterTitle(request.chapterTitle());
        chapter.setChapterIndex(request.chapterIndex());
        Chapter saved = chapterRepository.save(chapter);

        logger.info(
            "Updated chapter {}: title='{}', index={} by user {}",
            chapterId,
            saved.getChapterTitle(),
            saved.getChapterIndex(),
            authorId
        );

        return ChapterResponse.from(saved);
    }

    @Override
    @Transactional
    public void deleteChapter(UUID chapterId, UUID authorId) {
        Chapter chapter = findChapterOwnedBy(chapterId, authorId, "видалення");
        chapterRepository.delete(chapter);

        logger.info("Deleted chapter {} '{}' by user {}", chapterId, chapter.getChapterTitle(), authorId);
    }

    private Chapter findChapterOwnedBy(UUID chapterId, UUID authorId, String action) {
        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new ChapterNotFoundException(chapterId));
        Manuscript manuscript = chapter.getManuscript();
        if (!manuscript.getAuthorId().equals(authorId)) {
            throw new ForbiddenActionException("Автор не має прав на " + action + " розділів цього рукопису");
        }
        if (manuscript.getStatus() != ManuscriptStatus.IN_PROGRESS) {
            throw new InvalidStateTransitionException(
                    "Manuscript",
                    manuscript.getManuscriptId(),
                    manuscript.getStatus(),
                    ManuscriptStatus.IN_PROGRESS,
                    manuscript.getStatus().allowedTransitions()
            );
        }
        return chapter;
    }
}
