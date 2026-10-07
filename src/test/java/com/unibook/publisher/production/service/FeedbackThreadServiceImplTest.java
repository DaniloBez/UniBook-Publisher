package com.unibook.publisher.production.service;

import com.unibook.publisher.common.enums.UserRole;
import com.unibook.publisher.common.event.ThreadOpenedEvent;
import com.unibook.publisher.common.exception.business.EmptyRevisionTextException;
import com.unibook.publisher.common.exception.business.InvalidQuoteException;
import com.unibook.publisher.common.exception.business.InvalidQuotePositionException;
import com.unibook.publisher.common.exception.business.ThreadNotASuggestionException;
import com.unibook.publisher.common.exception.notfound.ChapterNotFoundException;
import com.unibook.publisher.common.exception.notfound.RevisionNotFoundException;
import com.unibook.publisher.common.exception.notfound.ThreadNotFoundException;
import com.unibook.publisher.common.exception.security.ForbiddenActionException;
import com.unibook.publisher.common.exception.state.InvalidStateTransitionException;
import com.unibook.publisher.common.exception.storage.FileReadException;
import com.unibook.publisher.common.logging.AppLogger;
import com.unibook.publisher.production.entity.Chapter;
import com.unibook.publisher.production.entity.FeedbackThread;
import com.unibook.publisher.production.entity.Manuscript;
import com.unibook.publisher.production.entity.Revision;
import com.unibook.publisher.production.entity.TeamAssignment;
import com.unibook.publisher.production.entity.request.OpenThreadRequest;
import com.unibook.publisher.production.entity.request.ThreadMessageRequest;
import com.unibook.publisher.production.entity.response.ThreadMessageResponse;
import com.unibook.publisher.production.entity.response.ThreadResponse;
import com.unibook.publisher.production.enums.ManuscriptStatus;
import com.unibook.publisher.production.enums.SuggestionStatus;
import com.unibook.publisher.production.enums.ThreadStatus;
import com.unibook.publisher.production.repository.*;
import com.unibook.publisher.storage.FileStorageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class FeedbackThreadServiceImplTest {

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
    private FileStorageService fileStorageService;

    @Mock
    private ApplicationEventPublisher publisher;

    @Mock
    private AppLogger logger;

    @InjectMocks
    private FeedbackThreadServiceImpl threadService;

    private FeedbackThread thread(UUID id, UUID createdBy, ThreadStatus status,
                                  String suggestedText, SuggestionStatus suggestionStatus) {
        FeedbackThread thread = new FeedbackThread(
                createdBy,
                suggestedText,
                null,
                null,
                null,
                null
        );
        thread.setId(id);
        thread.setStatus(status);
        if (suggestionStatus != null)
            thread.setSuggestionStatus(suggestionStatus);

        return thread;
    }

    private Chapter chapter(UUID chapterId, Manuscript manuscript) {
        Chapter chapter = new Chapter(manuscript, "Розділ 1", 1);
        chapter.setChapterId(chapterId);
        return chapter;
    }

    private Revision revision(UUID revisionId, String fileUrl) {
        Revision revision = new Revision(1, fileUrl, UUID.randomUUID(), Instant.now());
        revision.setRevisionId(revisionId);
        return revision;
    }

    @Test
    @DisplayName("Успішне відкриття треду автором розділу")
    void openThread_Success() {
        UUID chapterId = UUID.randomUUID();
        UUID manuscriptId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        Manuscript manuscript = new Manuscript(manuscriptId, "Дюна", authorId, ManuscriptStatus.IN_PROGRESS, Set.of(), "Анотація", "url", Instant.now());
        Chapter chapter = chapter(chapterId, manuscript);

        when(chapterRepository.findById(chapterId)).thenReturn(Optional.of(chapter));
        when(teamAssignmentRepository.findByManuscript_ManuscriptIdAndRole(any(), any())).thenReturn(Optional.empty());
        when(threadRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

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
        FeedbackThread thread = thread(threadId, UUID.randomUUID(), ThreadStatus.OPEN, "новий текст", SuggestionStatus.PENDING);
        Manuscript manuscript = new Manuscript(manuscriptId, "Дюна", UUID.randomUUID(), ManuscriptStatus.IN_PROGRESS, Set.of(), "Анотація", "url", Instant.now());
        Chapter chapter = chapter(chapterId, manuscript);
        chapter.addThread(thread);

        when(threadRepository.findById(threadId)).thenReturn(Optional.of(thread));

        assertThrows(ForbiddenActionException.class, () -> threadService.acceptSuggestion(threadId, UUID.randomUUID()));
        verify(threadRepository, never()).save(any());
    }

    @Test
    @DisplayName("Заборонено повторно закривати вже закритий тред")
    void resolveThread_InvalidStateWhenAlreadyResolved() {
        UUID threadId = UUID.randomUUID();
        UUID chapterId = UUID.randomUUID();
        UUID manuscriptId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        FeedbackThread thread = thread(threadId, authorId, ThreadStatus.RESOLVED, null, null);
        Manuscript manuscript = new Manuscript(manuscriptId, "Дюна", authorId, ManuscriptStatus.IN_PROGRESS, Set.of(), "Анотація", "url", Instant.now());
        Chapter chapter = chapter(chapterId, manuscript);
        chapter.addThread(thread);

        when(threadRepository.findById(threadId)).thenReturn(Optional.of(thread));
        when(teamAssignmentRepository.findByManuscript_ManuscriptIdAndRole(any(), any())).thenReturn(Optional.empty());

        assertThrows(InvalidStateTransitionException.class, () -> threadService.resolveThread(threadId, authorId));
        verify(threadRepository, never()).save(any());
    }

    @Test
    @DisplayName("Помилка відкриття треду, якщо розділ не знайдено")
    void openThread_ChapterNotFound() {
        UUID chapterId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();

        when(chapterRepository.findById(chapterId)).thenReturn(Optional.empty());

        assertThrows(ChapterNotFoundException.class,
                () -> threadService.openThread(chapterId, authorId, new OpenThreadRequest("Погляньте на цей абзац", null, null, null, null, null)));
        verify(threadRepository, never()).save(any());
    }

    @Test
    @DisplayName("Відкриття треду з пропозицією зберігає текст та статус PENDING")
    void openThread_SuggestionBranch() {
        UUID chapterId = UUID.randomUUID();
        UUID manuscriptId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        Manuscript manuscript = new Manuscript(manuscriptId, "Дюна", authorId, ManuscriptStatus.IN_PROGRESS, Set.of(), "Анотація", "url", Instant.now());
        Chapter chapter = chapter(chapterId, manuscript);

        when(chapterRepository.findById(chapterId)).thenReturn(Optional.of(chapter));
        when(teamAssignmentRepository.findByManuscript_ManuscriptIdAndRole(any(), any())).thenReturn(Optional.empty());
        when(threadRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ThreadResponse response = threadService.openThread(chapterId, authorId,
                new OpenThreadRequest("Погляньте на цей абзац", "новий текст", null, null, null, null));

        assertTrue(response.isSuggestion());
        assertEquals(SuggestionStatus.PENDING, response.suggestionStatus());
        assertEquals("новий текст", response.suggestedText());
    }

    @Test
    @DisplayName("Успішне отримання списку тредів розділу")
    void getThreads_Success() {
        UUID chapterId = UUID.randomUUID();
        UUID manuscriptId = UUID.randomUUID();
        Chapter chapter = chapter(chapterId, new Manuscript(manuscriptId, "Дюна", UUID.randomUUID(), ManuscriptStatus.IN_PROGRESS, Set.of(), "Анотація", "url", Instant.now()));
        FeedbackThread thread = thread(UUID.randomUUID(), UUID.randomUUID(), ThreadStatus.OPEN, null, null);
        chapter.addThread(thread);

        when(chapterRepository.findById(chapterId)).thenReturn(Optional.of(chapter));
        when(threadRepository.findByChapterIdWithMessages(chapterId)).thenReturn(List.of(thread));

        List<ThreadResponse> responses = threadService.getThreads(chapterId, null);

        assertEquals(1, responses.size());
        assertEquals(thread.getId(), responses.getFirst().id());
    }

    @Test
    @DisplayName("Помилка отримання тредів, якщо розділ не знайдено")
    void getThreads_ChapterNotFound() {
        UUID chapterId = UUID.randomUUID();

        when(chapterRepository.findById(chapterId)).thenReturn(Optional.empty());

        assertThrows(ChapterNotFoundException.class, () -> threadService.getThreads(chapterId, null));
    }

    @Test
    @DisplayName("Отримання тредів без фільтра статусу викликає findByChapterId")
    void getThreads_NoStatusFilter_UsesFindByChapterId() {
        UUID chapterId = UUID.randomUUID();
        UUID manuscriptId = UUID.randomUUID();
        Chapter chapter = chapter(chapterId, new Manuscript(manuscriptId, "Дюна", UUID.randomUUID(), ManuscriptStatus.IN_PROGRESS, Set.of(), "Анотація", "url", Instant.now()));

        when(chapterRepository.findById(chapterId)).thenReturn(Optional.of(chapter));
        when(threadRepository.findByChapterIdWithMessages(chapterId)).thenReturn(List.of());

        threadService.getThreads(chapterId, null);

        verify(threadRepository, times(1)).findByChapterIdWithMessages(chapterId);
        verify(threadRepository, never()).findByChapterIdAndStatusWithMessages(any(), any());
    }

    @Test
    @DisplayName("Отримання тредів з фільтром статусу викликає findByChapterIdAndStatus")
    void getThreads_WithStatusFilter_UsesFindByChapterIdAndStatus() {
        UUID chapterId = UUID.randomUUID();
        UUID manuscriptId = UUID.randomUUID();
        Chapter chapter = chapter(chapterId, new Manuscript(manuscriptId, "Дюна", UUID.randomUUID(), ManuscriptStatus.IN_PROGRESS, Set.of(), "Анотація", "url", Instant.now()));

        when(chapterRepository.findById(chapterId)).thenReturn(Optional.of(chapter));
        when(threadRepository.findByChapterIdAndStatusWithMessages(chapterId, ThreadStatus.RESOLVED)).thenReturn(List.of());

        threadService.getThreads(chapterId, ThreadStatus.RESOLVED);

        verify(threadRepository, times(1)).findByChapterIdAndStatusWithMessages(chapterId, ThreadStatus.RESOLVED);
        verify(threadRepository, never()).findByChapterIdWithMessages(any());
    }

    @Test
    @DisplayName("Успішне додавання повідомлення автором рукопису")
    void addMessage_SuccessByAuthor() {
        UUID threadId = UUID.randomUUID();
        UUID chapterId = UUID.randomUUID();
        UUID manuscriptId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        FeedbackThread thread = thread(threadId, authorId, ThreadStatus.OPEN, null, null);
        Manuscript manuscript = new Manuscript(manuscriptId, "Дюна", authorId, ManuscriptStatus.IN_PROGRESS, Set.of(), "Анотація", "url", Instant.now());
        Chapter chapter = chapter(chapterId, manuscript);
        chapter.addThread(thread);

        when(threadRepository.findById(threadId)).thenReturn(Optional.of(thread));
        when(teamAssignmentRepository.findByManuscript_ManuscriptIdAndRole(any(), any())).thenReturn(Optional.empty());
        when(messageRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ThreadMessageResponse response = threadService.addMessage(threadId, authorId, new ThreadMessageRequest("Дякую за фідбек"));

        assertEquals("Дякую за фідбек", response.content());
        verify(messageRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("Успішне додавання повідомлення призначеним редактором")
    void addMessage_SuccessByEditor() {
        UUID threadId = UUID.randomUUID();
        UUID chapterId = UUID.randomUUID();
        UUID manuscriptId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        UUID editorId = UUID.randomUUID();
        FeedbackThread thread = thread(threadId, authorId, ThreadStatus.OPEN, null, null);
        Manuscript manuscript = new Manuscript(manuscriptId, "Дюна", authorId, ManuscriptStatus.IN_PROGRESS, Set.of(), "Анотація", "url", Instant.now());
        Chapter chapter = chapter(chapterId, manuscript);
        chapter.addThread(thread);
        TeamAssignment editorAssignment = new TeamAssignment(UUID.randomUUID(), manuscript, editorId, UserRole.EDITOR, Instant.now());

        when(threadRepository.findById(threadId)).thenReturn(Optional.of(thread));
        when(teamAssignmentRepository.findByManuscript_ManuscriptIdAndRole(any(), any())).thenReturn(Optional.of(editorAssignment));
        when(messageRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ThreadMessageResponse response = threadService.addMessage(threadId, editorId, new ThreadMessageRequest("Виправлено"));

        assertEquals("Виправлено", response.content());
        verify(messageRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("Заборонено додавати повідомлення користувачу, який не є автором чи редактором")
    void addMessage_ForbiddenWhenNotAuthorOrEditor() {
        UUID threadId = UUID.randomUUID();
        UUID chapterId = UUID.randomUUID();
        UUID manuscriptId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        FeedbackThread thread = thread(threadId, authorId, ThreadStatus.OPEN, null, null);
        Manuscript manuscript = new Manuscript(manuscriptId, "Дюна", authorId, ManuscriptStatus.IN_PROGRESS, Set.of(), "Анотація", "url", Instant.now());
        Chapter chapter = chapter(chapterId, manuscript);
        chapter.addThread(thread);

        when(threadRepository.findById(threadId)).thenReturn(Optional.of(thread));
        when(teamAssignmentRepository.findByManuscript_ManuscriptIdAndRole(any(), any())).thenReturn(Optional.empty());

        assertThrows(ForbiddenActionException.class,
                () -> threadService.addMessage(threadId, UUID.randomUUID(), new ThreadMessageRequest("Хтось стороннiй")));
        verify(messageRepository, never()).save(any());
    }

    @Test
    @DisplayName("Помилка додавання повідомлення, якщо тред не знайдено")
    void addMessage_ThreadNotFound() {
        UUID threadId = UUID.randomUUID();

        when(threadRepository.findById(threadId)).thenReturn(Optional.empty());

        assertThrows(ThreadNotFoundException.class,
                () -> threadService.addMessage(threadId, UUID.randomUUID(), new ThreadMessageRequest("Текст")));
    }

    @Test
    @DisplayName("Успішне прийняття пропозиції автором рукопису")
    void acceptSuggestion_Success() {
        UUID threadId = UUID.randomUUID();
        UUID chapterId = UUID.randomUUID();
        UUID manuscriptId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        FeedbackThread thread = thread(threadId, UUID.randomUUID(), ThreadStatus.OPEN, "новий текст", SuggestionStatus.PENDING);
        Manuscript manuscript = new Manuscript(manuscriptId, "Дюна", authorId, ManuscriptStatus.IN_PROGRESS, Set.of(), "Анотація", "url", Instant.now());
        Chapter chapter = chapter(chapterId, manuscript);
        chapter.addThread(thread);

        when(threadRepository.findById(threadId)).thenReturn(Optional.of(thread));
        when(threadRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ThreadResponse response = threadService.acceptSuggestion(threadId, authorId);

        assertEquals(SuggestionStatus.ACCEPTED, response.suggestionStatus());
        verify(threadRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("Помилка прийняття пропозиції, якщо тред не знайдено")
    void acceptSuggestion_ThreadNotFound() {
        UUID threadId = UUID.randomUUID();

        when(threadRepository.findById(threadId)).thenReturn(Optional.empty());

        assertThrows(ThreadNotFoundException.class, () -> threadService.acceptSuggestion(threadId, UUID.randomUUID()));
    }

    @Test
    @DisplayName("Помилка прийняття, якщо тред не є пропозицією")
    void acceptSuggestion_NotASuggestion() {
        UUID threadId = UUID.randomUUID();
        UUID chapterId = UUID.randomUUID();
        UUID manuscriptId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        FeedbackThread thread = thread(threadId, UUID.randomUUID(), ThreadStatus.OPEN, null, null);
        Manuscript manuscript = new Manuscript(manuscriptId, "Дюна", authorId, ManuscriptStatus.IN_PROGRESS, Set.of(), "Анотація", "url", Instant.now());
        Chapter chapter = chapter(chapterId, manuscript);
        chapter.addThread(thread);

        when(threadRepository.findById(threadId)).thenReturn(Optional.of(thread));

        assertThrows(ThreadNotASuggestionException.class, () -> threadService.acceptSuggestion(threadId, authorId));
        verify(threadRepository, never()).save(any());
    }

    @Test
    @DisplayName("Помилка прийняття пропозиції, яка вже оброблена")
    void acceptSuggestion_InvalidStateWhenAlreadyProcessed() {
        UUID threadId = UUID.randomUUID();
        UUID chapterId = UUID.randomUUID();
        UUID manuscriptId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        FeedbackThread thread = thread(threadId, UUID.randomUUID(), ThreadStatus.OPEN, "новий текст", SuggestionStatus.ACCEPTED);
        Manuscript manuscript = new Manuscript(manuscriptId, "Дюна", authorId, ManuscriptStatus.IN_PROGRESS, Set.of(), "Анотація", "url", Instant.now());
        Chapter chapter = chapter(chapterId, manuscript);
        chapter.addThread(thread);

        when(threadRepository.findById(threadId)).thenReturn(Optional.of(thread));

        assertThrows(InvalidStateTransitionException.class, () -> threadService.acceptSuggestion(threadId, authorId));
        verify(threadRepository, never()).save(any());
    }

    @Test
    @DisplayName("Успішне відхилення пропозиції автором рукопису")
    void rejectSuggestion_Success() {
        UUID threadId = UUID.randomUUID();
        UUID chapterId = UUID.randomUUID();
        UUID manuscriptId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        FeedbackThread thread = thread(threadId, UUID.randomUUID(), ThreadStatus.OPEN, "новий текст", SuggestionStatus.PENDING);
        Manuscript manuscript = new Manuscript(manuscriptId, "Дюна", authorId, ManuscriptStatus.IN_PROGRESS, Set.of(), "Анотація", "url", Instant.now());
        Chapter chapter = chapter(chapterId, manuscript);
        chapter.addThread(thread);

        when(threadRepository.findById(threadId)).thenReturn(Optional.of(thread));
        when(threadRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ThreadResponse response = threadService.rejectSuggestion(threadId, authorId);

        assertEquals(SuggestionStatus.REJECTED, response.suggestionStatus());
        verify(threadRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("Заборонено відхиляти пропозицію не автору рукопису")
    void rejectSuggestion_ForbiddenWhenNotAuthor() {
        UUID threadId = UUID.randomUUID();
        UUID chapterId = UUID.randomUUID();
        UUID manuscriptId = UUID.randomUUID();
        FeedbackThread thread = thread(threadId, UUID.randomUUID(), ThreadStatus.OPEN, "новий текст", SuggestionStatus.PENDING);
        Manuscript manuscript = new Manuscript(manuscriptId, "Дюна", UUID.randomUUID(), ManuscriptStatus.IN_PROGRESS, Set.of(), "Анотація", "url", Instant.now());
        Chapter chapter = chapter(chapterId, manuscript);
        chapter.addThread(thread);

        when(threadRepository.findById(threadId)).thenReturn(Optional.of(thread));

        assertThrows(ForbiddenActionException.class, () -> threadService.rejectSuggestion(threadId, UUID.randomUUID()));
        verify(threadRepository, never()).save(any());
    }

    @Test
    @DisplayName("Помилка відхилення пропозиції, якщо тред не знайдено")
    void rejectSuggestion_ThreadNotFound() {
        UUID threadId = UUID.randomUUID();

        when(threadRepository.findById(threadId)).thenReturn(Optional.empty());

        assertThrows(ThreadNotFoundException.class, () -> threadService.rejectSuggestion(threadId, UUID.randomUUID()));
    }

    @Test
    @DisplayName("Помилка відхилення, якщо тред не є пропозицією")
    void rejectSuggestion_NotASuggestion() {
        UUID threadId = UUID.randomUUID();
        UUID chapterId = UUID.randomUUID();
        UUID manuscriptId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        FeedbackThread thread = thread(threadId, UUID.randomUUID(), ThreadStatus.OPEN, null, null);
        Manuscript manuscript = new Manuscript(manuscriptId, "Дюна", authorId, ManuscriptStatus.IN_PROGRESS, Set.of(), "Анотація", "url", Instant.now());
        Chapter chapter = chapter(chapterId, manuscript);
        chapter.addThread(thread);

        when(threadRepository.findById(threadId)).thenReturn(Optional.of(thread));

        assertThrows(ThreadNotASuggestionException.class, () -> threadService.rejectSuggestion(threadId, authorId));
        verify(threadRepository, never()).save(any());
    }

    @Test
    @DisplayName("Помилка відхилення пропозиції, яка вже оброблена")
    void rejectSuggestion_InvalidStateWhenAlreadyProcessed() {
        UUID threadId = UUID.randomUUID();
        UUID chapterId = UUID.randomUUID();
        UUID manuscriptId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        FeedbackThread thread = thread(threadId, UUID.randomUUID(), ThreadStatus.OPEN, "новий текст", SuggestionStatus.REJECTED);
        Manuscript manuscript = new Manuscript(manuscriptId, "Дюна", authorId, ManuscriptStatus.IN_PROGRESS, Set.of(), "Анотація", "url", Instant.now());
        Chapter chapter = chapter(chapterId, manuscript);
        chapter.addThread(thread);

        when(threadRepository.findById(threadId)).thenReturn(Optional.of(thread));

        assertThrows(InvalidStateTransitionException.class, () -> threadService.rejectSuggestion(threadId, authorId));
        verify(threadRepository, never()).save(any());
    }

    @Test
    @DisplayName("Успішне закриття треду автором рукопису")
    void resolveThread_Success() {
        UUID threadId = UUID.randomUUID();
        UUID chapterId = UUID.randomUUID();
        UUID manuscriptId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        FeedbackThread thread = thread(threadId, authorId, ThreadStatus.OPEN, null, null);
        Manuscript manuscript = new Manuscript(manuscriptId, "Дюна", authorId, ManuscriptStatus.IN_PROGRESS, Set.of(), "Анотація", "url", Instant.now());
        Chapter chapter = chapter(chapterId, manuscript);
        chapter.addThread(thread);

        when(threadRepository.findById(threadId)).thenReturn(Optional.of(thread));
        when(teamAssignmentRepository.findByManuscript_ManuscriptIdAndRole(any(), any())).thenReturn(Optional.empty());
        when(threadRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ThreadResponse response = threadService.resolveThread(threadId, authorId);

        assertEquals(ThreadStatus.RESOLVED, response.status());
        verify(threadRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("Помилка закриття треду, якщо тред не знайдено")
    void resolveThread_ThreadNotFound() {
        UUID threadId = UUID.randomUUID();

        when(threadRepository.findById(threadId)).thenReturn(Optional.empty());

        assertThrows(ThreadNotFoundException.class, () -> threadService.resolveThread(threadId, UUID.randomUUID()));
    }

    @Test
    @DisplayName("Заборонено закривати тред користувачу, який не є автором чи призначеним редактором")
    void resolveThread_ForbiddenWhenNotAuthorOrEditor() {
        UUID threadId = UUID.randomUUID();
        UUID chapterId = UUID.randomUUID();
        UUID manuscriptId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        FeedbackThread thread = thread(threadId, authorId, ThreadStatus.OPEN, null, null);
        Manuscript manuscript = new Manuscript(manuscriptId, "Дюна", authorId, ManuscriptStatus.IN_PROGRESS, Set.of(), "Анотація", "url", Instant.now());
        Chapter chapter = chapter(chapterId, manuscript);
        chapter.addThread(thread);

        when(threadRepository.findById(threadId)).thenReturn(Optional.of(thread));
        when(teamAssignmentRepository.findByManuscript_ManuscriptIdAndRole(any(), any())).thenReturn(Optional.empty());

        assertThrows(ForbiddenActionException.class, () -> threadService.resolveThread(threadId, UUID.randomUUID()));
        verify(threadRepository, never()).save(any());
    }

    @Test
    @DisplayName("Помилка валідації цитати: ревізію не знайдено")
    void validateQuote_ThrowsRevisionNotFoundException() {
        UUID chapterId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        UUID manuscriptId = UUID.randomUUID();
        UUID revisionId = UUID.randomUUID();

        Manuscript manuscript = new Manuscript(manuscriptId, "Книга", authorId, ManuscriptStatus.IN_PROGRESS, Set.of(), "Опис", "url", Instant.now());
        Chapter chapter = chapter(chapterId, manuscript);

        when(chapterRepository.findById(chapterId)).thenReturn(Optional.of(chapter));
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
    @DisplayName("Перевірка цитати пропускається, якщо targetRevisionId або quotedText дорівнює null")
    void validateQuote_Success_Return() {
        UUID chapterId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        UUID manuscriptId = UUID.randomUUID();
        UUID revisionId = UUID.randomUUID();

        Manuscript manuscript = new Manuscript(manuscriptId, "Книга", authorId, ManuscriptStatus.IN_PROGRESS, Set.of(), "Опис", "url", Instant.now());
        Chapter chapter = chapter(chapterId, manuscript);

        when(chapterRepository.findById(chapterId)).thenReturn(Optional.of(chapter));
        when(threadRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

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
    @DisplayName("Помилка валідації цитати: файл ревізії порожній")
    void validateQuote_ThrowsEmptyRevisionTextException() {
        UUID chapterId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        UUID manuscriptId = UUID.randomUUID();
        UUID revisionId = UUID.randomUUID();

        Manuscript manuscript = new Manuscript(manuscriptId, "Книга", authorId, ManuscriptStatus.IN_PROGRESS, Set.of(), "Опис", "url", Instant.now());
        Chapter chapter = chapter(chapterId, manuscript);
        Revision revision = revision(revisionId, "path/to/empty.txt");

        when(chapterRepository.findById(chapterId)).thenReturn(Optional.of(chapter));
        when(revisionRepository.findById(revisionId)).thenReturn(Optional.of(revision));
        when(fileStorageService.get("path/to/empty.txt")).thenReturn(new ByteArrayInputStream(new byte[0]));

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
    @DisplayName("Помилка валідації цитати: некоректні позиції символів from/to")
    void validateQuote_ThrowsInvalidQuotePositionException() {
        UUID chapterId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        UUID manuscriptId = UUID.randomUUID();
        UUID revisionId = UUID.randomUUID();

        Manuscript manuscript = new Manuscript(manuscriptId, "Книга", authorId, ManuscriptStatus.IN_PROGRESS, Set.of(), "Опис", "url", Instant.now());
        Chapter chapter = chapter(chapterId, manuscript);
        Revision revision = revision(revisionId, "path/to/file.txt");

        when(chapterRepository.findById(chapterId)).thenReturn(Optional.of(chapter));
        when(revisionRepository.findById(revisionId)).thenReturn(Optional.of(revision));
        when(fileStorageService.get("path/to/file.txt"))
                .thenAnswer(_ -> new ByteArrayInputStream("Текст для перевірки".getBytes(StandardCharsets.UTF_8)));

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
    @DisplayName("Помилка валідації цитати: текст у позиції не збігається з переданою цитатою")
    void validateQuote_ThrowsInvalidQuoteException() {
        UUID chapterId = UUID.randomUUID();
        UUID manuscriptId = UUID.randomUUID();
        UUID revisionId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();

        Manuscript manuscript = new Manuscript(manuscriptId, "Книга", authorId, ManuscriptStatus.IN_PROGRESS, Set.of(), "Опис", "url", Instant.now());
        Chapter chapter = chapter(chapterId, manuscript);
        Revision revision = revision(revisionId, "path/to/file.txt");

        when(chapterRepository.findById(chapterId)).thenReturn(Optional.of(chapter));
        when(revisionRepository.findById(revisionId)).thenReturn(Optional.of(revision));
        when(fileStorageService.get("path/to/file.txt"))
                .thenReturn(new ByteArrayInputStream("Текст для перевірки".getBytes(StandardCharsets.UTF_8)));

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
    @DisplayName("Успішна валідація цитати: текст у позиції повністю збігається")
    void validateQuote_NotThrowsInvalidQuoteException() {
        UUID chapterId = UUID.randomUUID();
        UUID manuscriptId = UUID.randomUUID();
        UUID revisionId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();

        Manuscript manuscript = new Manuscript(manuscriptId, "Книга", authorId, ManuscriptStatus.IN_PROGRESS, Set.of(), "Опис", "url", Instant.now());
        Chapter chapter = chapter(chapterId, manuscript);
        Revision revision = revision(revisionId, "path/to/file.txt");

        when(chapterRepository.findById(chapterId)).thenReturn(Optional.of(chapter));
        when(revisionRepository.findById(revisionId)).thenReturn(Optional.of(revision));
        when(fileStorageService.get("path/to/file.txt"))
                .thenReturn(new ByteArrayInputStream("Текст для перевірки".getBytes(StandardCharsets.UTF_8)));
        when(threadRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

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

    @Test
    @DisplayName("Успішне видалення треду автором рукопису")
    void deleteThread_SuccessByAuthor() {
        UUID threadId = UUID.randomUUID();
        UUID chapterId = UUID.randomUUID();
        UUID manuscriptId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        FeedbackThread thread = thread(threadId, authorId, ThreadStatus.OPEN, null, null);
        Manuscript manuscript = new Manuscript(manuscriptId, "Дюна", authorId, ManuscriptStatus.IN_PROGRESS, Set.of(), "Анотація", "url", Instant.now());
        chapter(chapterId, manuscript).addThread(thread);

        when(threadRepository.findById(threadId)).thenReturn(Optional.of(thread));
        when(teamAssignmentRepository.findByManuscript_ManuscriptIdAndRole(any(), any())).thenReturn(Optional.empty());

        threadService.deleteThread(threadId, authorId);

        verify(threadRepository, times(1)).delete(thread);
    }

    @Test
    @DisplayName("Успішне видалення треду призначеним редактором")
    void deleteThread_SuccessByEditor() {
        UUID threadId = UUID.randomUUID();
        UUID chapterId = UUID.randomUUID();
        UUID manuscriptId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        UUID editorId = UUID.randomUUID();
        FeedbackThread thread = thread(threadId, authorId, ThreadStatus.OPEN, null, null);
        Manuscript manuscript = new Manuscript(manuscriptId, "Дюна", authorId, ManuscriptStatus.IN_PROGRESS, Set.of(), "Анотація", "url", Instant.now());
        chapter(chapterId, manuscript).addThread(thread);
        TeamAssignment editorAssignment = new TeamAssignment(UUID.randomUUID(), manuscript, editorId, UserRole.EDITOR, Instant.now());

        when(threadRepository.findById(threadId)).thenReturn(Optional.of(thread));
        when(teamAssignmentRepository.findByManuscript_ManuscriptIdAndRole(any(), any())).thenReturn(Optional.of(editorAssignment));

        threadService.deleteThread(threadId, editorId);

        verify(threadRepository, times(1)).delete(thread);
    }

    @Test
    @DisplayName("Заборонено видаляти тред користувачу, який не є автором чи призначеним редактором")
    void deleteThread_ForbiddenWhenNotAuthorOrEditor() {
        UUID threadId = UUID.randomUUID();
        UUID chapterId = UUID.randomUUID();
        UUID manuscriptId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        FeedbackThread thread = thread(threadId, authorId, ThreadStatus.OPEN, null, null);
        Manuscript manuscript = new Manuscript(manuscriptId, "Дюна", authorId, ManuscriptStatus.IN_PROGRESS, Set.of(), "Анотація", "url", Instant.now());
        chapter(chapterId, manuscript).addThread(thread);

        when(threadRepository.findById(threadId)).thenReturn(Optional.of(thread));
        when(teamAssignmentRepository.findByManuscript_ManuscriptIdAndRole(any(), any())).thenReturn(Optional.empty());

        assertThrows(ForbiddenActionException.class, () -> threadService.deleteThread(threadId, UUID.randomUUID()));
        verify(threadRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Помилка видалення треду, якщо тред не знайдено")
    void deleteThread_ThreadNotFound() {
        UUID threadId = UUID.randomUUID();

        when(threadRepository.findById(threadId)).thenReturn(Optional.empty());

        assertThrows(ThreadNotFoundException.class, () -> threadService.deleteThread(threadId, UUID.randomUUID()));
        verify(threadRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Помилка валідації цитати: помилка читання файлу зі сховища (FileReadException)")
    void validateQuote_ThrowsFileReadException_WhenInputStreamFails() {
        UUID chapterId = UUID.randomUUID();
        UUID manuscriptId = UUID.randomUUID();
        UUID revisionId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();

        Manuscript manuscript = new Manuscript(manuscriptId, "Книга", authorId, ManuscriptStatus.IN_PROGRESS, Set.of(), "Опис", "url", Instant.now());
        Chapter chapter = chapter(chapterId, manuscript);
        Revision revision = revision(revisionId, "path/to/corrupted.txt");

        when(chapterRepository.findById(chapterId)).thenReturn(Optional.of(chapter));
        when(revisionRepository.findById(revisionId)).thenReturn(Optional.of(revision));

        InputStream failingStream = new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException("S3 connection interrupted");
            }
        };
        when(fileStorageService.get("path/to/corrupted.txt")).thenReturn(failingStream);

        OpenThreadRequest request = new OpenThreadRequest(
                "Початкове повідомлення",
                null,
                revisionId,
                "Цитата",
                0,
                5
        );

        assertThrows(FileReadException.class, () -> threadService.openThread(chapterId, authorId, request));
    }

    @Test
    @DisplayName("Успішне відкриття треду призначеним редактором (перевірка адресата сповіщення)")
    void openThread_SuccessByEditor() {
        UUID chapterId = UUID.randomUUID();
        UUID manuscriptId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        UUID editorId = UUID.randomUUID();

        Manuscript manuscript = new Manuscript(manuscriptId, "Дюна", authorId, ManuscriptStatus.IN_PROGRESS, Set.of(), "Анотація", "url", Instant.now());
        Chapter chapter = chapter(chapterId, manuscript);
        TeamAssignment editorAssignment = new TeamAssignment(UUID.randomUUID(), manuscript, editorId, UserRole.EDITOR, Instant.now());

        when(chapterRepository.findById(chapterId)).thenReturn(Optional.of(chapter));
        when(teamAssignmentRepository.findByManuscript_ManuscriptIdAndRole(manuscriptId, UserRole.EDITOR)).thenReturn(Optional.of(editorAssignment));
        when(threadRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ThreadResponse response = threadService.openThread(chapterId, editorId, new OpenThreadRequest("Зауваження від редактора", null, null, null, null, null));

        assertEquals(ThreadStatus.OPEN, response.status());
        verify(publisher, times(1)).publishEvent(any(ThreadOpenedEvent.class));
    }
}