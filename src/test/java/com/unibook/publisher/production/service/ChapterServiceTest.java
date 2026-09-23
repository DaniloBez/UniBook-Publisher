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
import com.unibook.publisher.production.entity.request.RevisionUploadRequest;
import com.unibook.publisher.production.entity.response.ChapterResponse;
import com.unibook.publisher.production.entity.response.RevisionResponse;
import com.unibook.publisher.production.repository.ChapterRepository;
import com.unibook.publisher.production.repository.ManuscriptRepository;
import com.unibook.publisher.production.repository.RevisionRepository;
import com.unibook.publisher.production.repository.TeamAssignmentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ChapterServiceTest {

    @Mock
    private ChapterRepository chapterRepository;

    @Mock
    private ManuscriptRepository manuscriptRepository;

    @Mock
    private RevisionRepository revisionRepository;

    @Mock
    private TeamAssignmentRepository teamAssignmentRepository;

    @Mock
    private ApplicationEventPublisher publisher;

    @Mock
    private AppLogger logger;

    @Mock
    private DiffService diffService;

    @InjectMocks
    private ChapterService chapterService;

    @Test
    void createChapter_Success() {
        UUID manuscriptId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        Manuscript manuscript = new Manuscript(
                manuscriptId,
                "451 градус по Фаренгейту",
                authorId,
                ManuscriptStatus.IN_PROGRESS,
                List.of(),
                "Опис до книги 451 градус по Фаренгейту",
                "url",
                Instant.now()
        );
        ChapterCreationRequest request = new ChapterCreationRequest("Розділ 1", 1);

        when(manuscriptRepository.findById(manuscriptId)).thenReturn(Optional.of(manuscript));
        when(chapterRepository.save(any(Chapter.class))).thenAnswer(i -> i.getArgument(0));

        ChapterResponse response = chapterService.createChapter(manuscriptId, authorId, request);
        assertNotNull(response);
        assertEquals("Розділ 1", response.chapterTitle());
        assertEquals(1, response.chapterIndex());
        verify(chapterRepository, times(1)).save(any(Chapter.class));
    }

    @Test
    void createChapter_ManuscriptNotFoundException() {
        UUID manuscriptId = UUID.randomUUID();
        when(manuscriptRepository.findById(manuscriptId)).thenReturn(Optional.empty());
        assertThrows(ManuscriptNotFoundException.class, () -> chapterService.createChapter(manuscriptId, UUID.randomUUID(), new ChapterCreationRequest("Розділ", 1)));
    }

    @Test
    void createChapter_ForbiddenActionException() {
        UUID manuscriptId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Manuscript manuscript = new Manuscript(
                manuscriptId,
                "451 градус по Фаренгейту",
                authorId,
                ManuscriptStatus.IN_PROGRESS,
                List.of(),
                "Опис до книги 451 градус по Фаренгейту",
                "url",
                Instant.now()
        );
        when(manuscriptRepository.findById(manuscriptId)).thenReturn(Optional.of(manuscript));
        assertThrows(ForbiddenActionException.class, () -> chapterService.createChapter(manuscriptId, userId, new ChapterCreationRequest("Розділ", 1)));
    }

    @Test
    void createChapter_InvalidStateTransitionException() {
        UUID manuscriptId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        Manuscript manuscript = new Manuscript(
                manuscriptId,
                "451 градус по Фаренгейту",
                authorId,
                ManuscriptStatus.SUBMITTED,
                List.of(),
                "Опис до книги 451 градус по Фаренгейту",
                "url",
                Instant.now()
        );
        when(manuscriptRepository.findById(manuscriptId)).thenReturn(Optional.of(manuscript));
        assertThrows(InvalidStateTransitionException.class, () -> chapterService.createChapter(manuscriptId, authorId, new ChapterCreationRequest("Розділ", 1)));
    }

    @Test
    void getChaptersByManuscriptId_Success() {
        UUID manuscriptId = UUID.randomUUID();
        Chapter chapter = new Chapter(UUID.randomUUID(), manuscriptId, "Розділ 1", 1);
        when(manuscriptRepository.findById(manuscriptId)).thenReturn(Optional.of(mock(Manuscript.class)));
        when(chapterRepository.findByManuscriptId(manuscriptId)).thenReturn(List.of(chapter));
        List<ChapterResponse> response = chapterService.getChaptersByManuscriptId(manuscriptId);
        assertNotNull(response);
        assertEquals(1, response.size());
        assertEquals("Розділ 1", response.get(0).chapterTitle());
    }

    @Test
    void getChaptersByManuscriptId_ManuscriptNotFoundException() {
        UUID manuscriptId = UUID.randomUUID();
        when(manuscriptRepository.findById(manuscriptId)).thenReturn(Optional.empty());
        assertThrows(ManuscriptNotFoundException.class, () -> chapterService.getChaptersByManuscriptId(manuscriptId));
    }

    @Test
    void uploadRevision_Editor_Success() {
        UUID chapterId = UUID.randomUUID();
        UUID manuscriptId = UUID.randomUUID();
        UUID editorId = UUID.randomUUID();
        Chapter chapter = new Chapter(chapterId, manuscriptId, "Розділ 1", 1);
        Manuscript manuscript = new Manuscript(
                manuscriptId,
                "451 градус по Фаренгейту",
                UUID.randomUUID(),
                ManuscriptStatus.IN_PROGRESS,
                List.of(),
                "Опис до книги 451 градус по Фаренгейту",
                "url",
                Instant.now()
        );

        when(chapterRepository.findById(chapterId)).thenReturn(Optional.of(chapter));
        when(manuscriptRepository.findById(manuscriptId)).thenReturn(Optional.of(manuscript));
        when(teamAssignmentRepository.isUserAssignedToManuscript(manuscriptId, editorId, UserRole.EDITOR)).thenReturn(true);
        when(revisionRepository.findLatestVersionNumberByChapterId(chapterId)).thenReturn(Optional.empty());
        when(revisionRepository.save(any(Revision.class))).thenAnswer(i -> i.getArgument(0));

        RevisionResponse response = chapterService.uploadRevision(chapterId, editorId, new RevisionUploadRequest("new_url"));
        assertNotNull(response);
        assertEquals(1, response.versionNumber());
        assertEquals("new_url", response.fileUrl());

        verify(publisher, times(1)).publishEvent(any(RevisionAddedEvent.class));
    }

    @Test
    void uploadRevision_Author_Success() {
        UUID chapterId = UUID.randomUUID();
        UUID manuscriptId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        Chapter chapter = new Chapter(chapterId, manuscriptId, "Розділ 1", 1);
        Manuscript manuscript = new Manuscript(
                manuscriptId,
                "451 градус по Фаренгейту",
                authorId,
                ManuscriptStatus.IN_PROGRESS,
                List.of(),
                "Опис до книги 451 градус по Фаренгейту",
                "url",
                Instant.now()
        );

        when(chapterRepository.findById(chapterId)).thenReturn(Optional.of(chapter));
        when(manuscriptRepository.findById(manuscriptId)).thenReturn(Optional.of(manuscript));
        when(revisionRepository.findLatestVersionNumberByChapterId(chapterId)).thenReturn(Optional.empty());
        when(revisionRepository.save(any(Revision.class))).thenAnswer(i -> i.getArgument(0));

        RevisionResponse response = chapterService.uploadRevision(chapterId, authorId, new RevisionUploadRequest("new_url"));
        assertNotNull(response);
        assertEquals(1, response.versionNumber());
        assertEquals("new_url", response.fileUrl());

        verify(publisher, times(1)).publishEvent(any(RevisionAddedEvent.class));
    }

    @Test
    void uploadRevision_ForbiddenActionException() {
        UUID chapterId = UUID.randomUUID();
        UUID manuscriptId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        Chapter chapter = new Chapter(chapterId, manuscriptId, "Глава 1", 1);
        Manuscript manuscript = new Manuscript(
                manuscriptId,
                "451 градус по Фаренгейту",
                UUID.randomUUID(),
                ManuscriptStatus.IN_PROGRESS,
                List.of(),
                "Опис до книги 451 градус по Фаренгейту",
                "url",
                Instant.now()
        );

        when(chapterRepository.findById(chapterId)).thenReturn(Optional.of(chapter));
        when(manuscriptRepository.findById(manuscriptId)).thenReturn(Optional.of(manuscript));
        when(teamAssignmentRepository.isUserAssignedToManuscript(manuscriptId, userId, UserRole.EDITOR)).thenReturn(false);

        assertThrows(ForbiddenActionException.class, () -> chapterService.uploadRevision(chapterId, userId, new RevisionUploadRequest("url")));
    }

    @Test
    void uploadRevision_ManuscriptNotFoundException() {
        UUID chapterId = UUID.randomUUID();
        Chapter chapter = new Chapter(chapterId, UUID.randomUUID(), "Розділ 1", 1);
        when(chapterRepository.findById(chapterId)).thenReturn(Optional.of(chapter));
        when(manuscriptRepository.findById(chapter.manuscriptId())).thenReturn(Optional.empty());
        assertThrows(ManuscriptNotFoundException.class, () -> chapterService.uploadRevision(chapterId, UUID.randomUUID(), new RevisionUploadRequest("url")));
    }

    @Test
    void uploadRevision_ChapterNotFoundException() {
        UUID chapterId = UUID.randomUUID();
        when(chapterRepository.findById(chapterId)).thenReturn(Optional.empty());
        assertThrows(ChapterNotFoundException.class, () -> chapterService.uploadRevision(chapterId, UUID.randomUUID(), new RevisionUploadRequest("url")));
    }

    @Test
    void uploadRevision_InvalidStateTransitionException() {
        UUID chapterId = UUID.randomUUID();
        UUID manuscriptId = UUID.randomUUID();
        Chapter chapter = new Chapter(chapterId, manuscriptId, "Розділ 1", 1);
        Manuscript manuscript = new Manuscript(
                manuscriptId,
                "451 градус по Фаренгейту",
                UUID.randomUUID(),
                ManuscriptStatus.SUBMITTED,
                List.of(),
                "Опис до книги 451 градус по Фаренгейту",
                "url",
                Instant.now()
        );
        when(chapterRepository.findById(chapterId)).thenReturn(Optional.of(chapter));
        when(manuscriptRepository.findById(manuscriptId)).thenReturn(Optional.of(manuscript));
        assertThrows(InvalidStateTransitionException.class, () -> chapterService.uploadRevision(chapterId, UUID.randomUUID(), new RevisionUploadRequest("url")));
    }

    @Test
    void getRevisionsByChapterId_Success() {
        UUID chapterId = UUID.randomUUID();
        Revision revision = new Revision(UUID.randomUUID(), chapterId, 1, "url", UUID.randomUUID(), Instant.now(), "Текст ревізії");

        when(chapterRepository.findById(chapterId)).thenReturn(Optional.of(mock(Chapter.class)));
        when(revisionRepository.findAllByChapterId(chapterId)).thenReturn(List.of(revision));

        List<RevisionResponse> revisions = chapterService.getRevisionsByChapterId(chapterId);

        assertNotNull(revisions);
        assertEquals(1, revisions.size());
        assertEquals(1, revisions.get(0).versionNumber());
    }

    @Test
    void getRevisionsByChapterId_ChapterNotFoundException() {
        UUID chapterId = UUID.randomUUID();
        when(chapterRepository.findById(chapterId)).thenReturn(Optional.empty());
        assertThrows(ChapterNotFoundException.class, () -> chapterService.getRevisionsByChapterId(chapterId));
    }

    @Test
    void getDiffChapter_Success() {
        UUID chapterId = UUID.randomUUID();
        UUID fromRevisionId = UUID.randomUUID();
        UUID toRevisionId = UUID.randomUUID();

        DiffRequest request = new DiffRequest(fromRevisionId, toRevisionId);

        Chapter chapter = new Chapter(chapterId, UUID.randomUUID(), "Розділ 1", 1);
        Revision fromRevision = new Revision(fromRevisionId, chapterId, 1, "url1", UUID.randomUUID(), Instant.now(), "Старий текст");
        Revision toRevision = new Revision(toRevisionId, chapterId, 2, "url2", UUID.randomUUID(), Instant.now(), "Новий текст");
        DiffResponse expectedResponse = new DiffResponse(fromRevisionId, toRevisionId, List.of());

        when(chapterRepository.findById(chapterId)).thenReturn(Optional.of(chapter));
        when(revisionRepository.findById(fromRevisionId)).thenReturn(Optional.of(fromRevision));
        when(revisionRepository.findById(toRevisionId)).thenReturn(Optional.of(toRevision));
        when(diffService.compare(fromRevisionId, toRevisionId, "Старий текст", "Новий текст")).thenReturn(expectedResponse);

        DiffResponse response = chapterService.getDiffChapter(chapterId, request);
        assertNotNull(response);
        assertEquals(expectedResponse, response);
        verify(diffService, times(1)).compare(fromRevisionId, toRevisionId, "Старий текст", "Новий текст");
    }

    @Test
    void getDiffChapter_ChapterNotFoundException() {
        UUID chapterId = UUID.randomUUID();
        DiffRequest request = new DiffRequest(UUID.randomUUID(), UUID.randomUUID());
        when(chapterRepository.findById(chapterId)).thenReturn(Optional.empty());
        assertThrows(ChapterNotFoundException.class, () -> chapterService.getDiffChapter(chapterId, request));
    }

    @Test
    void getDiffChapter_FromRevisionNotFoundException() {
        UUID chapterId = UUID.randomUUID();
        UUID fromRevisionId = UUID.randomUUID();
        DiffRequest request = new DiffRequest(fromRevisionId, UUID.randomUUID());
        when(chapterRepository.findById(chapterId)).thenReturn(Optional.of(mock(Chapter.class)));
        when(revisionRepository.findById(fromRevisionId)).thenReturn(Optional.empty());
        assertThrows(RevisionNotFoundException.class, () -> chapterService.getDiffChapter(chapterId, request));
    }

    @Test
    void getDiffChapter_ToRevisionNotFoundException() {
        UUID chapterId = UUID.randomUUID();
        UUID fromRevisionId = UUID.randomUUID();
        UUID toRevisionId = UUID.randomUUID();
        DiffRequest request = new DiffRequest(fromRevisionId, toRevisionId);
        when(chapterRepository.findById(chapterId)).thenReturn(Optional.of(mock(Chapter.class)));
        when(revisionRepository.findById(fromRevisionId)).thenReturn(Optional.of(mock(Revision.class)));
        when(revisionRepository.findById(toRevisionId)).thenReturn(Optional.empty());
        assertThrows(RevisionNotFoundException.class, () -> chapterService.getDiffChapter(chapterId, request));
    }

    @Test
    void getDiffChapter_From_BusinessRuleViolationException() {
        UUID chapterId = UUID.randomUUID();
        UUID fromRevisionId = UUID.randomUUID();
        UUID toRevisionId = UUID.randomUUID();

        DiffRequest request = new DiffRequest(fromRevisionId, toRevisionId);
        Chapter chapter = new Chapter(chapterId, UUID.randomUUID(), "Розділ 1", 1);
        Revision fromRevision = new Revision(fromRevisionId, UUID.randomUUID(), 1, "url1", UUID.randomUUID(), Instant.now(), "Старий текст");
        Revision toRevision = new Revision(toRevisionId, chapterId, 2, "url2", UUID.randomUUID(), Instant.now(), "Новий текст");

        when(chapterRepository.findById(chapterId)).thenReturn(Optional.of(chapter));
        when(revisionRepository.findById(fromRevisionId)).thenReturn(Optional.of(fromRevision));
        when(revisionRepository.findById(toRevisionId)).thenReturn(Optional.of(toRevision));
        assertThrows(BusinessRuleViolationException.class, () -> chapterService.getDiffChapter(chapterId, request));
    }

    @Test
    void getDiffChapter_To_BusinessRuleViolationException() {
        UUID chapterId = UUID.randomUUID();
        UUID fromRevisionId = UUID.randomUUID();
        UUID toRevisionId = UUID.randomUUID();

        DiffRequest request = new DiffRequest(fromRevisionId, toRevisionId);
        Chapter chapter = new Chapter(chapterId, UUID.randomUUID(), "Розділ 1", 1);
        Revision fromRevision = new Revision(fromRevisionId, chapterId, 1, "url1", UUID.randomUUID(), Instant.now(), "Старий текст");
        Revision toRevision = new Revision(toRevisionId, UUID.randomUUID(), 2, "url2", UUID.randomUUID(), Instant.now(), "Новий текст");

        when(chapterRepository.findById(chapterId)).thenReturn(Optional.of(chapter));
        when(revisionRepository.findById(fromRevisionId)).thenReturn(Optional.of(fromRevision));
        when(revisionRepository.findById(toRevisionId)).thenReturn(Optional.of(toRevision));
        assertThrows(BusinessRuleViolationException.class, () -> chapterService.getDiffChapter(chapterId, request));
    }
}
