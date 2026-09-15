package com.unibook.publisher.production.service;

import com.unibook.publisher.common.exception.InvalidStateTransitionException;
import com.unibook.publisher.common.exception.ResourceNotFoundException;
import com.unibook.publisher.production.entity.Chapter;
import com.unibook.publisher.production.entity.Manuscript;
import com.unibook.publisher.production.entity.ManuscriptStatus;
import com.unibook.publisher.production.entity.Revision;
import com.unibook.publisher.production.entity.request.ChapterCreationRequest;
import com.unibook.publisher.production.entity.request.RevisionUploadRequest;
import com.unibook.publisher.production.entity.response.ChapterResponse;
import com.unibook.publisher.production.entity.response.RevisionResponse;
import com.unibook.publisher.production.repository.ChapterRepository;
import com.unibook.publisher.production.repository.ManuscriptRepository;
import com.unibook.publisher.production.repository.RevisionRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class ChapterService {
    private final ChapterRepository chapterRepository;
    private final ManuscriptRepository manuscriptRepository;
    private final RevisionRepository revisionRepository;

    public ChapterService(ChapterRepository chapterRepository, ManuscriptRepository manuscriptRepository, RevisionRepository revisionRepository) {
        this.chapterRepository = chapterRepository;
        this.manuscriptRepository = manuscriptRepository;
        this.revisionRepository = revisionRepository;
    }

    public ChapterResponse createChapter(UUID manuscriptId, UUID authorId, ChapterCreationRequest request) {
        Manuscript manuscript = manuscriptRepository.findById(manuscriptId)
                .orElseThrow(() -> new ResourceNotFoundException("Рукопис за ID: " + manuscriptId + " не знайдено"));
        if(!manuscript.authorId().equals(authorId)) {
            throw new IllegalArgumentException("Автор не має прав на додавання розділів до цього рукопису");
        }
        if(manuscript.status() != ManuscriptStatus.IN_PROGRESS) {
            throw new InvalidStateTransitionException("Створення розділів доступне лише у статусі IN_PROGRESS");
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
            throw new ResourceNotFoundException("Рукопис за ID: " + manuscriptId + " не знайдено");
        }
        return chapterRepository.findByManuscriptId(manuscriptId).stream()
                .map(ChapterResponse::from)
                .toList();
    }

    public RevisionResponse uploadRevision(UUID chapterId, UUID userId, RevisionUploadRequest request) {
        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new ResourceNotFoundException("Розділ за ID: " + chapterId + " не знайдено"));
        Manuscript manuscript = manuscriptRepository.findById(chapter.manuscriptId())
                .orElseThrow(() -> new ResourceNotFoundException("Рукопис за ID: " + chapter.manuscriptId() + " не знайдено"));
        if(manuscript.status() != ManuscriptStatus.IN_PROGRESS) {
            throw new InvalidStateTransitionException("Завантаження ревізій доступне лише у статусі IN_PROGRESS");
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
        return RevisionResponse.from(saved);
    }

    public List<RevisionResponse> getRevisionsByChapterId(UUID chapterId) {
        if(chapterRepository.findById(chapterId).isEmpty()) {
            throw new ResourceNotFoundException("Розділ за ID: " + chapterId + " не знайдено");
        }
        return revisionRepository.findAllByChapterId(chapterId).stream()
                .map(RevisionResponse::from)
                .toList();
    }
}
