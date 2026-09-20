package com.unibook.publisher.production.service;

import com.unibook.publisher.common.enums.UserRole;
import com.unibook.publisher.common.event.RevisionAddedEvent;
import com.unibook.publisher.common.exception.notfound.ChapterNotFoundException;
import com.unibook.publisher.common.exception.notfound.ManuscriptNotFoundException;
import com.unibook.publisher.common.exception.security.ForbiddenActionException;
import com.unibook.publisher.common.exception.state.InvalidStateTransitionException;
import com.unibook.publisher.production.entity.Chapter;
import com.unibook.publisher.production.entity.Manuscript;
import com.unibook.publisher.production.enums.ManuscriptStatus;
import com.unibook.publisher.production.entity.Revision;
import com.unibook.publisher.production.entity.request.ChapterCreationRequest;
import com.unibook.publisher.production.entity.request.RevisionUploadRequest;
import com.unibook.publisher.production.entity.response.ChapterResponse;
import com.unibook.publisher.production.entity.response.RevisionResponse;
import com.unibook.publisher.production.repository.ChapterRepository;
import com.unibook.publisher.production.repository.ManuscriptRepository;
import com.unibook.publisher.production.repository.RevisionRepository;
import com.unibook.publisher.production.repository.TeamAssignmentRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class ChapterService {
    private final ChapterRepository chapterRepository;
    private final ManuscriptRepository manuscriptRepository;
    private final RevisionRepository revisionRepository;
    private final TeamAssignmentRepository teamAssignmentRepository;
    private final ApplicationEventPublisher publisher;

    public ChapterService(ChapterRepository chapterRepository, ManuscriptRepository manuscriptRepository, RevisionRepository revisionRepository, TeamAssignmentRepository teamAssignmentRepository, ApplicationEventPublisher publisher) {
        this.chapterRepository = chapterRepository;
        this.manuscriptRepository = manuscriptRepository;
        this.revisionRepository = revisionRepository;
        this.teamAssignmentRepository = teamAssignmentRepository;
        this.publisher = publisher;
    }

    public ChapterResponse createChapter(UUID manuscriptId, UUID authorId, ChapterCreationRequest request) {
        Manuscript manuscript = manuscriptRepository.findById(manuscriptId)
                .orElseThrow(() -> new ManuscriptNotFoundException(manuscriptId));
        if(!manuscript.authorId().equals(authorId)) {
            throw new ForbiddenActionException("Автор не має прав на додавання розділів до цього рукопису");
        }
        if(manuscript.status() != ManuscriptStatus.IN_PROGRESS) {
            throw new InvalidStateTransitionException(
                    "Manuscript",
                    manuscriptId,
                    manuscript.status(),
                    ManuscriptStatus.IN_PROGRESS,
                    manuscript.status().allowedTransitions()
            );
        }
        Chapter chapter = new Chapter(
                UUID.randomUUID(),
                manuscriptId,
                request.chapterTitle(),
                request.chapterIndex()
        );
        Chapter saved = chapterRepository.save(chapter);
        return ChapterResponse.from(saved);
    }

    public List<ChapterResponse> getChaptersByManuscriptId(UUID manuscriptId) {
        if(manuscriptRepository.findById(manuscriptId).isEmpty()) {
            throw new ManuscriptNotFoundException(manuscriptId);
        }
        return chapterRepository.findByManuscriptId(manuscriptId).stream()
                .map(ChapterResponse::from)
                .toList();
    }

    public RevisionResponse uploadRevision(UUID chapterId, UUID userId, RevisionUploadRequest request) {
        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new ChapterNotFoundException(chapterId));
        Manuscript manuscript = manuscriptRepository.findById(chapter.manuscriptId())
                .orElseThrow(() -> new ManuscriptNotFoundException(chapter.manuscriptId()));
        if(manuscript.status() != ManuscriptStatus.IN_PROGRESS) {
            throw new InvalidStateTransitionException(
                    "Manuscript",
                    manuscript.manuscriptId(),
                    manuscript.status(),
                    ManuscriptStatus.IN_PROGRESS,
                    manuscript.status().allowedTransitions()
            );
        }

        boolean isAuthor = manuscript.authorId().equals(userId);
        boolean isEditor = teamAssignmentRepository.isUserAssignedToManuscript(manuscript.manuscriptId(), userId, UserRole.EDITOR);

        if(!isAuthor && !isEditor) {
            throw new ForbiddenActionException("Користувач не має прав на завантаження ревізій для цього розділу");
        }

        int versionNumber = revisionRepository.findLatestVersionNumberByChapterId(chapterId)
                .map(revision -> revision.versionNumber() + 1)
                .orElse(1);

        Revision revision = new Revision(
                UUID.randomUUID(),
                chapterId,
                versionNumber,
                request.fileUrl(),
                userId,
                Instant.now()
        );
        Revision saved = revisionRepository.save(revision);
        publisher.publishEvent(new RevisionAddedEvent(
                manuscript.manuscriptId(),
                manuscript.title(),
                chapter.chapterId(),
                saved.revisionId(),
                userId,
                manuscript.authorId()
        ));

        return RevisionResponse.from(saved);
    }

    public List<RevisionResponse> getRevisionsByChapterId(UUID chapterId) {
        if(chapterRepository.findById(chapterId).isEmpty()) {
            throw new ChapterNotFoundException(chapterId);
        }
        return revisionRepository.findAllByChapterId(chapterId).stream()
                .map(RevisionResponse::from)
                .toList();
    }
}
