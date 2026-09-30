package com.unibook.publisher.production.service;

import com.unibook.publisher.common.enums.UserRole;
import com.unibook.publisher.common.event.RevisionAddedEvent;
import com.unibook.publisher.common.exception.business.BusinessRuleViolationException;
import com.unibook.publisher.common.exception.notfound.ChapterNotFoundException;
import com.unibook.publisher.common.exception.notfound.ManuscriptNotFoundException;
import com.unibook.publisher.common.exception.notfound.RevisionNotFoundException;
import com.unibook.publisher.common.exception.security.ForbiddenActionException;
import com.unibook.publisher.common.exception.state.InvalidStateTransitionException;
import com.unibook.publisher.common.logging.AppLogger;
import com.unibook.publisher.production.entity.Chapter;
import com.unibook.publisher.production.entity.Manuscript;
import com.unibook.publisher.production.entity.request.DiffRequest;
import com.unibook.publisher.production.entity.response.DiffResponse;
import com.unibook.publisher.production.enums.ManuscriptStatus;
import com.unibook.publisher.production.entity.Revision;
import com.unibook.publisher.production.entity.request.ChapterCreationRequest;
import com.unibook.publisher.production.entity.request.ChapterUpdateRequest;
import com.unibook.publisher.production.entity.request.RevisionUploadRequest;
import com.unibook.publisher.production.entity.response.ChapterResponse;
import com.unibook.publisher.production.entity.response.RevisionResponse;
import com.unibook.publisher.production.repository.ChapterRepository;
import com.unibook.publisher.production.repository.ManuscriptRepository;
import com.unibook.publisher.production.repository.RevisionRepository;
import com.unibook.publisher.production.repository.TeamAssignmentRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ChapterServiceImpl implements ChapterService {
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
    public RevisionResponse uploadRevision(UUID chapterId, UUID userId, RevisionUploadRequest request) {
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

        if(!isAuthor && !isEditor) {
            throw new ForbiddenActionException("Користувач не має прав на завантаження ревізій для цього розділу");
        }

        int versionNumber = revisionRepository.findTopByChapter_ChapterIdOrderByVersionNumberDesc(chapterId)
                .map(revision -> revision.getVersionNumber() + 1)
                .orElse(1);

        String textContent = fileStorageService.getAsText(request.fileUrl());  //REM Convenient but somewhat dangerous way to fill up all our server RAM with 100MB strings    xD

        Revision revision = new Revision(versionNumber, request.fileUrl(), userId, Instant.now(), textContent);
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
        if (chapterRepository.findById(chapterId).isEmpty()) {
            throw new ChapterNotFoundException(chapterId);
        }
        Revision fromRevision = revisionRepository.findById(request.fromRevisionId())
                .orElseThrow(() -> new RevisionNotFoundException(request.fromRevisionId()));
        Revision toRevision = revisionRepository.findById(request.toRevisionId())
                .orElseThrow(() -> new RevisionNotFoundException(request.toRevisionId()));

        if (!fromRevision.getChapterId().equals(chapterId) || !toRevision.getChapterId().equals(chapterId)) {
            throw new BusinessRuleViolationException("Запитані ревізії не належать розділу з ID: " + chapterId);
        }

        return diffService.compare(
                request.fromRevisionId(),
                request.toRevisionId(),
                fromRevision.getTextContent(),
                toRevision.getTextContent()
        );
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
