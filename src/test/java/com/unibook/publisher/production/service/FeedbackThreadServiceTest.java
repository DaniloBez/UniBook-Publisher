package com.unibook.publisher.production.service;

import com.unibook.publisher.common.event.ThreadOpenedEvent;
import com.unibook.publisher.common.exception.business.EmptyRevisionTextException;
import com.unibook.publisher.common.exception.business.InvalidQuoteException;
import com.unibook.publisher.common.exception.business.InvalidQuotePositionException;
import com.unibook.publisher.common.exception.notfound.RevisionNotFoundException;
import com.unibook.publisher.common.exception.security.ForbiddenActionException;
import com.unibook.publisher.common.exception.state.InvalidStateTransitionException;
import com.unibook.publisher.common.logging.AppLogger;
import com.unibook.publisher.production.entity.*;
import com.unibook.publisher.production.enums.ManuscriptStatus;
import com.unibook.publisher.production.entity.request.OpenThreadRequest;
import com.unibook.publisher.production.entity.response.ThreadResponse;
import com.unibook.publisher.production.enums.SuggestionStatus;
import com.unibook.publisher.production.enums.ThreadStatus;
import com.unibook.publisher.production.repository.*;
import org.junit.jupiter.api.DisplayName;
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
public class FeedbackThreadServiceTest {

    @Mock
    private FeedbackThreadRepository threadRepository;

    @Mock
    private ThreadMessageRepository messageRepository;

    @Mock
    private ChapterRepository chapterRepository;

    @Mock
    private ManuscriptRepository manuscriptRepository;

    @Mock
    private TeamAssignmentRepository teamAssignmentRepository;

    @Mock
    private RevisionRepository revisionRepository;

    @Mock
    private ApplicationEventPublisher publisher;

    @Mock
    private AppLogger logger;

    @InjectMocks
    private FeedbackThreadService threadService;

    @Test
    @DisplayName("Успішне відкриття треду автором розділу")
    void openThread_Success() {
        UUID chapterId = UUID.randomUUID();
        UUID manuscriptId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        Chapter chapter = new Chapter(chapterId, manuscriptId, "Розділ 1", 1);
        Manuscript manuscript = new Manuscript(manuscriptId, "Дюна", authorId, ManuscriptStatus.IN_PROGRESS, List.of(), "Анотація", "url", Instant.now());

        when(chapterRepository.findById(chapterId)).thenReturn(Optional.of(chapter));
        when(manuscriptRepository.findById(manuscriptId)).thenReturn(Optional.of(manuscript));
        when(teamAssignmentRepository.findByManuscriptIdAndRole(any(), any())).thenReturn(Optional.empty());
        when(threadRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(messageRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ThreadResponse response = threadService.openThread(chapterId, authorId, new OpenThreadRequest("Погляньте на цей абзац", null, null, null, null, null));

        assertEquals(ThreadStatus.OPEN, response.status());
        assertFalse(response.isSuggestion());
        verify(publisher, times(1)).publishEvent(any(ThreadOpenedEvent.class));
    }

    @Test
    @DisplayName("Заборонено приймати пропозицію не автору рукопису")
    void acceptSuggestion_ForbiddenWhenNotAuthor() {
        UUID threadId = UUID.randomUUID();
        UUID chapterId = UUID.randomUUID();
        UUID manuscriptId = UUID.randomUUID();
        FeedbackThread thread = new FeedbackThread(
                threadId,
                chapterId,
                UUID.randomUUID(),
                ThreadStatus.OPEN,
                true,
                "новий текст",
                SuggestionStatus.PENDING,
                Instant.now(),
                null,
                null,
                null,
                null);
        Chapter chapter = new Chapter(chapterId, manuscriptId, "Розділ 1", 1);
        Manuscript manuscript = new Manuscript(manuscriptId, "Дюна", UUID.randomUUID(), ManuscriptStatus.IN_PROGRESS, List.of(), "Анотація", "url", Instant.now());

        when(threadRepository.findById(threadId)).thenReturn(Optional.of(thread));
        when(chapterRepository.findById(chapterId)).thenReturn(Optional.of(chapter));
        when(manuscriptRepository.findById(manuscriptId)).thenReturn(Optional.of(manuscript));

        assertThrows(ForbiddenActionException.class, () -> threadService.acceptSuggestion(threadId, UUID.randomUUID()));
        verify(threadRepository, never()).save(any());
    }

    @Test
    void acceptSuggestion_ThrowsRevisionNotFoundException() {
        UUID threadId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        UUID chapterId = UUID.randomUUID();
        UUID manuscriptId = UUID.randomUUID();
        UUID revisionId = UUID.randomUUID();

        FeedbackThread thread = new FeedbackThread(
                threadId,
                chapterId,
                UUID.randomUUID(),
                ThreadStatus.OPEN,
                true,
                "Текст",
                SuggestionStatus.PENDING,
                Instant.now(),
                revisionId,
                "Цитата",
                0,
                5
        );
        Chapter chapter = new Chapter(chapterId, manuscriptId, "Розділ 1", 1);
        Manuscript manuscript = new Manuscript(manuscriptId, "Книга", authorId, ManuscriptStatus.IN_PROGRESS, List.of(), "Опис", "url", Instant.now());

        when(threadRepository.findById(threadId)).thenReturn(Optional.of(thread));
        when(chapterRepository.findById(chapterId)).thenReturn(Optional.of(chapter));
        when(manuscriptRepository.findById(manuscriptId)).thenReturn(Optional.of(manuscript));
        when(revisionRepository.findById(revisionId)).thenReturn(Optional.empty());

        assertThrows(RevisionNotFoundException.class, () -> threadService.acceptSuggestion(threadId, authorId));
    }

    @Test
    void acceptSuggestion_Success_WithEmptySuggestedText() {
        UUID threadId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        UUID chapterId = UUID.randomUUID();
        UUID manuscriptId = UUID.randomUUID();
        UUID revisionId = UUID.randomUUID();

        FeedbackThread thread = new FeedbackThread(
                threadId,
                chapterId,
                UUID.randomUUID(),
                ThreadStatus.OPEN,
                true,
                null, //suggestedText = null
                SuggestionStatus.PENDING,
                Instant.now(),
                revisionId,
                "",
                0,
                5
        );
        Chapter chapter = new Chapter(chapterId, manuscriptId, "Розділ 1", 1);
        Manuscript manuscript = new Manuscript(manuscriptId, "Книга", authorId, ManuscriptStatus.IN_PROGRESS, List.of(), "Опис", "url", Instant.now());
        Revision revision = new Revision(revisionId,chapterId, 1, "url", authorId, Instant.now(), "Текст для перевірки");

        when(threadRepository.findById(threadId)).thenReturn(Optional.of(thread));
        when(chapterRepository.findById(chapterId)).thenReturn(Optional.of(chapter));
        when(manuscriptRepository.findById(manuscriptId)).thenReturn(Optional.of(manuscript));
        when(revisionRepository.findById(revisionId)).thenReturn(Optional.of(revision));
        when(threadRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        assertDoesNotThrow(() -> threadService.acceptSuggestion(threadId, authorId));
    }

    @Test
    void acceptSuggestion_Success_WithEmptyTextContent() {
        UUID threadId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        UUID chapterId = UUID.randomUUID();
        UUID manuscriptId = UUID.randomUUID();
        UUID revisionId = UUID.randomUUID();

        FeedbackThread thread = new FeedbackThread(
                threadId,
                chapterId,
                UUID.randomUUID(),
                ThreadStatus.OPEN,
                true,
                "Текст",
                SuggestionStatus.PENDING,
                Instant.now(),
                revisionId,
                "",
                0,
                0
        );
        Chapter chapter = new Chapter(chapterId, manuscriptId, "Розділ 1", 1);
        Manuscript manuscript = new Manuscript(manuscriptId, "Книга", authorId, ManuscriptStatus.IN_PROGRESS, List.of(), "Опис", "url", Instant.now());
        Revision revision = new Revision(revisionId, chapterId, 1, "url", authorId, Instant.now(), null);  // textContent = null

        when(threadRepository.findById(threadId)).thenReturn(Optional.of(thread));
        when(chapterRepository.findById(chapterId)).thenReturn(Optional.of(chapter));
        when(manuscriptRepository.findById(manuscriptId)).thenReturn(Optional.of(manuscript));
        when(revisionRepository.findById(revisionId)).thenReturn(Optional.of(revision));
        when(threadRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        assertDoesNotThrow(() -> threadService.acceptSuggestion(threadId, authorId));
    }

    @Test
    void acceptSuggestion_Success_WithInvalidBoundsInvalid() {
        UUID threadId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        UUID chapterId = UUID.randomUUID();
        UUID manuscriptId = UUID.randomUUID();
        UUID revisionId = UUID.randomUUID();

        Chapter chapter = new Chapter(chapterId, manuscriptId, "Розділ 1", 1);
        Manuscript manuscript = new Manuscript(manuscriptId, "Книга", authorId, ManuscriptStatus.IN_PROGRESS, List.of(), "Опис", "url", Instant.now());
        Revision revision = new Revision(revisionId, chapterId, 1, "url", authorId, Instant.now(), "Короткий текст");

        when(chapterRepository.findById(chapterId)).thenReturn(Optional.of(chapter));
        when(manuscriptRepository.findById(manuscriptId)).thenReturn(Optional.of(manuscript));
        when(revisionRepository.findById(revisionId)).thenReturn(Optional.of(revision));
        when(threadRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        int[][] invalidPositions = {
                {-1, 5},
                {0, 50},
                {10, 5}
        };
        for (int[] position : invalidPositions) {
            FeedbackThread thread = new FeedbackThread(
                    threadId,
                    chapterId,
                    UUID.randomUUID(),
                    ThreadStatus.OPEN,
                    true,
                    "Текст",
                    SuggestionStatus.PENDING,
                    Instant.now(),
                    revisionId,
                    "Цитата",
                    position[0],
                    position[1]
            );
            when(threadRepository.findById(threadId)).thenReturn(Optional.of(thread));
            assertDoesNotThrow(() -> threadService.acceptSuggestion(threadId, authorId));
        }
        verify(revisionRepository, never()).save(any());
    }

    @Test
    void acceptSuggestion_Success_WithNullParameters() {
        UUID threadId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        UUID chapterId = UUID.randomUUID();
        UUID manuscriptId = UUID.randomUUID();
        UUID revisionId = UUID.randomUUID();

        Chapter chapter = new Chapter(chapterId, manuscriptId, "Розділ 1", 1);
        Manuscript manuscript = new Manuscript(manuscriptId, "Книга", authorId, ManuscriptStatus.IN_PROGRESS, List.of(), "Опис", "url", Instant.now());

        when(chapterRepository.findById(chapterId)).thenReturn(Optional.of(chapter));
        when(manuscriptRepository.findById(manuscriptId)).thenReturn(Optional.of(manuscript));

        FeedbackThread[] threadsWithNulls = {
                new FeedbackThread(threadId, chapterId, UUID.randomUUID(), ThreadStatus.OPEN, true, "Текст", SuggestionStatus.PENDING, Instant.now(), null, "Цитата", 0, 5), //targetRevisionId == null
                new FeedbackThread(threadId, chapterId, UUID.randomUUID(), ThreadStatus.OPEN, true, "Текст", SuggestionStatus.PENDING, Instant.now(), revisionId, "Цитата", null, 5), //positionFrom == null
                new FeedbackThread(threadId, chapterId, UUID.randomUUID(), ThreadStatus.OPEN, true, "Текст", SuggestionStatus.PENDING, Instant.now(), revisionId, "Цитата", 0, null) //positionTo == null
        };

        for (FeedbackThread thread : threadsWithNulls) {
            when(threadRepository.findById(threadId)).thenReturn(Optional.of(thread));
            assertDoesNotThrow(() -> threadService.acceptSuggestion(threadId, authorId));
        }
        verify(revisionRepository, never()).findById(any());
        verify(revisionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Заборонено повторно закривати вже закритий тред")
    void resolveThread_InvalidStateWhenAlreadyResolved() {
        UUID threadId = UUID.randomUUID();
        UUID chapterId = UUID.randomUUID();
        UUID manuscriptId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        FeedbackThread thread = new FeedbackThread(
                threadId,
                chapterId,
                authorId,
                ThreadStatus.RESOLVED,
                false,
                null,
                null,
                Instant.now(),
                null,
                null,
                null,
                null
        );
        Chapter chapter = new Chapter(chapterId, manuscriptId, "Розділ 1", 1);
        Manuscript manuscript = new Manuscript(manuscriptId, "Дюна", authorId, ManuscriptStatus.IN_PROGRESS, List.of(), "Анотація", "url", Instant.now());

        when(threadRepository.findById(threadId)).thenReturn(Optional.of(thread));
        when(chapterRepository.findById(chapterId)).thenReturn(Optional.of(chapter));
        when(manuscriptRepository.findById(manuscriptId)).thenReturn(Optional.of(manuscript));
        when(teamAssignmentRepository.findByManuscriptIdAndRole(any(), any())).thenReturn(Optional.empty());

        assertThrows(InvalidStateTransitionException.class, () -> threadService.resolveThread(threadId, authorId));
        verify(threadRepository, never()).save(any());
    }

    @Test
    void validateQuote_ThrowsRevisionNotFoundException() {
        UUID chapterId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        UUID manuscriptId = UUID.randomUUID();
        UUID revisionId = UUID.randomUUID();

        Chapter chapter = new Chapter(chapterId, manuscriptId, "Розділ 1", 1);
        Manuscript manuscript = new Manuscript(manuscriptId, "Книга", authorId, ManuscriptStatus.IN_PROGRESS, List.of(), "Опис", "url", Instant.now());

        when(chapterRepository.findById(chapterId)).thenReturn(Optional.of(chapter));
        when(manuscriptRepository.findById(manuscriptId)).thenReturn(Optional.of(manuscript));
        when(revisionRepository.findById(revisionId)).thenReturn(Optional.empty());

        OpenThreadRequest request = new OpenThreadRequest(
                "Початкове повідомлення",
                null,
                revisionId,
                "Цитата",
                0,
                5
        );
        assertThrows(RevisionNotFoundException.class, () -> threadService.openThread(chapterId, authorId, request));
    }

    @Test
    void validateQuote_Success_Return() {
        UUID chapterId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        UUID manuscriptId = UUID.randomUUID();
        UUID revisionId = UUID.randomUUID();

        Chapter chapter = new Chapter(chapterId, manuscriptId, "Розділ 1", 1);
        Manuscript manuscript = new Manuscript(manuscriptId, "Книга", authorId, ManuscriptStatus.IN_PROGRESS, List.of(), "Опис", "url", Instant.now());

        when(chapterRepository.findById(chapterId)).thenReturn(Optional.of(chapter));
        when(manuscriptRepository.findById(manuscriptId)).thenReturn(Optional.of(manuscript));
        when(threadRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(messageRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        OpenThreadRequest request1 = new OpenThreadRequest(
                "Початкове повідомлення",
                null,
                null, // targetRevisionId = null
                "Цитата",
                0,
                5
        );
        assertDoesNotThrow(() -> threadService.openThread(chapterId, authorId, request1));

        OpenThreadRequest request2 = new OpenThreadRequest(
                "Початкове повідомлення",
                null,
                revisionId,
                null, // quotedText = null
                0,
                5
        );
        assertDoesNotThrow(() -> threadService.openThread(chapterId, authorId, request2));
        verify(revisionRepository, never()).findById(any());
    }

    @Test
    void validateQuote_ThrowsEmptyRevisionTextException() {
        UUID chapterId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        UUID manuscriptId = UUID.randomUUID();
        UUID revisionId = UUID.randomUUID();

        Chapter chapter = new Chapter(chapterId, manuscriptId, "Розділ 1", 1);
        Manuscript manuscript = new Manuscript(manuscriptId, "Книга", authorId, ManuscriptStatus.IN_PROGRESS, List.of(), "Опис", "url", Instant.now());
        Revision revision = new Revision(revisionId, chapterId, 1, "url", authorId, Instant.now(), null);

        when(chapterRepository.findById(chapterId)).thenReturn(Optional.of(chapter));
        when(manuscriptRepository.findById(manuscriptId)).thenReturn(Optional.of(manuscript));
        when(revisionRepository.findById(revisionId)).thenReturn(Optional.of(revision));

        OpenThreadRequest request = new OpenThreadRequest(
                "Початкове повідомлення",
                null,
                revisionId,
                "Цитата",
                0,
                5
        );

        assertThrows(EmptyRevisionTextException.class, () -> threadService.openThread(chapterId, authorId, request));
    }

    @Test
    void validateQuote_ThrowsInvalidQuotePositionException() {
        UUID chapterId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        UUID manuscriptId = UUID.randomUUID();
        UUID revisionId = UUID.randomUUID();

        Chapter chapter = new Chapter(chapterId, manuscriptId, "Розділ 1", 1);
        Manuscript manuscript = new Manuscript(manuscriptId, "Книга", authorId, ManuscriptStatus.IN_PROGRESS, List.of(), "Опис", "url", Instant.now());
        Revision revision = new Revision(revisionId, chapterId, 1, "url", authorId, Instant.now(), "Текст для перевірки");

        when(chapterRepository.findById(chapterId)).thenReturn(Optional.of(chapter));
        when(manuscriptRepository.findById(manuscriptId)).thenReturn(Optional.of(manuscript));
        when(revisionRepository.findById(revisionId)).thenReturn(Optional.of(revision));

        OpenThreadRequest request1 = new OpenThreadRequest(
                "Початкове повідомлення",
                null,
                revisionId,
                "Текст",
                0,
                100
        );

        OpenThreadRequest request2 = new OpenThreadRequest(
                "Початкове повідомлення",
                null, revisionId,
                "Текст",
                10,
                5
        );

        OpenThreadRequest request3 = new OpenThreadRequest(
                "Початкове повідомлення",
                null, revisionId,
                "Текст",
                null,
                5
        );

        OpenThreadRequest request4 = new OpenThreadRequest(
                "Початкове повідомлення",
                null, revisionId,
                "Текст",
                0,
                null
        );

        OpenThreadRequest request5 = new OpenThreadRequest(
                "Початкове повідомлення",
                null, revisionId,
                "Текст",
                -1,
                5
        );

        assertThrows(InvalidQuotePositionException.class, () -> threadService.openThread(chapterId, authorId, request1));
        assertThrows(InvalidQuotePositionException.class, () -> threadService.openThread(chapterId, authorId, request2));
        assertThrows(InvalidQuotePositionException.class, () -> threadService.openThread(chapterId, authorId, request3));
        assertThrows(InvalidQuotePositionException.class, () -> threadService.openThread(chapterId, authorId, request4));
        assertThrows(InvalidQuotePositionException.class, () -> threadService.openThread(chapterId, authorId, request5));
    }

    @Test
    void validateQuote_ThrowsInvalidQuoteException() {
        UUID chapterId = UUID.randomUUID();
        UUID manuscriptId = UUID.randomUUID();
        UUID revisionId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();

        Chapter chapter = new Chapter(chapterId, manuscriptId, "Розділ 1", 1);
        Manuscript manuscript = new Manuscript(manuscriptId, "Книга", authorId, ManuscriptStatus.IN_PROGRESS, List.of(), "Опис", "url", Instant.now());
        Revision revision = new Revision(revisionId, chapterId, 1, "url", authorId, Instant.now(), "Текст для перевірки");

        when(chapterRepository.findById(chapterId)).thenReturn(Optional.of(chapter));
        when(manuscriptRepository.findById(manuscriptId)).thenReturn(Optional.of(manuscript));
        when(revisionRepository.findById(revisionId)).thenReturn(Optional.of(revision));

        OpenThreadRequest request = new OpenThreadRequest(
                "Початкове повідомлення",
                null,
                revisionId,
                "Сонце",
                0,
                5
        );
        assertThrows(InvalidQuoteException.class, () -> threadService.openThread(chapterId, authorId, request));
    }

    @Test
    void validateQuote_NotThrowsInvalidQuoteException() {
        UUID chapterId = UUID.randomUUID();
        UUID manuscriptId = UUID.randomUUID();
        UUID revisionId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();

        Chapter chapter = new Chapter(chapterId, manuscriptId, "Розділ 1", 1);
        Manuscript manuscript = new Manuscript(manuscriptId, "Книга", authorId, ManuscriptStatus.IN_PROGRESS, List.of(), "Опис", "url", Instant.now());
        Revision revision = new Revision(revisionId, chapterId, 1, "url", authorId, Instant.now(), "Текст для перевірки");

        when(chapterRepository.findById(chapterId)).thenReturn(Optional.of(chapter));
        when(manuscriptRepository.findById(manuscriptId)).thenReturn(Optional.of(manuscript));
        when(revisionRepository.findById(revisionId)).thenReturn(Optional.of(revision));
        when(threadRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(messageRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        OpenThreadRequest request = new OpenThreadRequest(
                "Початкове повідомлення",
                null,
                revisionId,
                "Текст",
                0,
                5
        );
        assertDoesNotThrow(() -> threadService.openThread(chapterId, authorId, request));
    }
}