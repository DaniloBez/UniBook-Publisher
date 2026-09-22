package com.unibook.publisher.production.service;

import com.unibook.publisher.common.event.ThreadOpenedEvent;
import com.unibook.publisher.common.exception.business.ThreadNotASuggestionException;
import com.unibook.publisher.common.exception.notfound.ChapterNotFoundException;
import com.unibook.publisher.common.exception.notfound.ManuscriptNotFoundException;
import com.unibook.publisher.common.exception.notfound.ThreadNotFoundException;
import com.unibook.publisher.common.exception.security.ForbiddenActionException;
import com.unibook.publisher.common.exception.state.InvalidStateTransitionException;
import com.unibook.publisher.common.logging.AppLogger;
import com.unibook.publisher.production.entity.Chapter;
import com.unibook.publisher.production.entity.FeedbackThread;
import com.unibook.publisher.production.entity.Manuscript;
import com.unibook.publisher.production.entity.TeamAssignment;
import com.unibook.publisher.production.entity.ThreadMessage;
import com.unibook.publisher.production.enums.ManuscriptStatus;
import com.unibook.publisher.production.entity.request.OpenThreadRequest;
import com.unibook.publisher.production.entity.request.ThreadMessageRequest;
import com.unibook.publisher.production.entity.response.ThreadMessageResponse;
import com.unibook.publisher.production.entity.response.ThreadResponse;
import com.unibook.publisher.production.enums.SuggestionStatus;
import com.unibook.publisher.production.enums.ThreadStatus;
import com.unibook.publisher.common.enums.UserRole;
import com.unibook.publisher.production.repository.ChapterRepository;
import com.unibook.publisher.production.repository.FeedbackThreadRepository;
import com.unibook.publisher.production.repository.ManuscriptRepository;
import com.unibook.publisher.production.repository.TeamAssignmentRepository;
import com.unibook.publisher.production.repository.ThreadMessageRepository;
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
    private ApplicationEventPublisher publisher;

    @Mock
    private AppLogger logger;

    @InjectMocks
    private FeedbackThreadServiceImpl threadService;

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

        ThreadResponse response = threadService.openThread(chapterId, authorId, new OpenThreadRequest("Погляньте на цей абзац", null));

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
        FeedbackThread thread = new FeedbackThread(threadId, chapterId, UUID.randomUUID(), ThreadStatus.OPEN,
                true, "новий текст", SuggestionStatus.PENDING, Instant.now());
        Chapter chapter = new Chapter(chapterId, manuscriptId, "Розділ 1", 1);
        Manuscript manuscript = new Manuscript(manuscriptId, "Дюна", UUID.randomUUID(), ManuscriptStatus.IN_PROGRESS, List.of(), "Анотація", "url", Instant.now());

        when(threadRepository.findById(threadId)).thenReturn(Optional.of(thread));
        when(chapterRepository.findById(chapterId)).thenReturn(Optional.of(chapter));
        when(manuscriptRepository.findById(manuscriptId)).thenReturn(Optional.of(manuscript));

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
        FeedbackThread thread = new FeedbackThread(threadId, chapterId, authorId, ThreadStatus.RESOLVED,
                false, null, null, Instant.now());
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
    @DisplayName("Помилка відкриття треду, якщо розділ не знайдено")
    void openThread_ChapterNotFound() {
        UUID chapterId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();

        when(chapterRepository.findById(chapterId)).thenReturn(Optional.empty());

        assertThrows(ChapterNotFoundException.class,
                () -> threadService.openThread(chapterId, authorId, new OpenThreadRequest("Погляньте на цей абзац", null)));
        verify(threadRepository, never()).save(any());
    }

    @Test
    @DisplayName("Помилка відкриття треду, якщо рукопис не знайдено")
    void openThread_ManuscriptNotFound() {
        UUID chapterId = UUID.randomUUID();
        UUID manuscriptId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        Chapter chapter = new Chapter(chapterId, manuscriptId, "Розділ 1", 1);

        when(chapterRepository.findById(chapterId)).thenReturn(Optional.of(chapter));
        when(manuscriptRepository.findById(manuscriptId)).thenReturn(Optional.empty());

        assertThrows(ManuscriptNotFoundException.class,
                () -> threadService.openThread(chapterId, authorId, new OpenThreadRequest("Погляньте на цей абзац", null)));
        verify(threadRepository, never()).save(any());
    }

    @Test
    @DisplayName("Відкриття треду з пропозицією зберігає текст та статус PENDING")
    void openThread_SuggestionBranch() {
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

        ThreadResponse response = threadService.openThread(chapterId, authorId,
                new OpenThreadRequest("Погляньте на цей абзац", "новий текст"));

        assertTrue(response.isSuggestion());
        assertEquals(SuggestionStatus.PENDING, response.suggestionStatus());
        assertEquals("новий текст", response.suggestedText());
    }

    @Test
    @DisplayName("Успішне отримання списку тредів розділу")
    void getThreads_Success() {
        UUID chapterId = UUID.randomUUID();
        UUID manuscriptId = UUID.randomUUID();
        Chapter chapter = new Chapter(chapterId, manuscriptId, "Розділ 1", 1);
        FeedbackThread thread = new FeedbackThread(UUID.randomUUID(), chapterId, UUID.randomUUID(),
                ThreadStatus.OPEN, false, null, null, Instant.now());

        when(chapterRepository.findById(chapterId)).thenReturn(Optional.of(chapter));
        when(threadRepository.findByChapterId(chapterId)).thenReturn(List.of(thread));

        List<ThreadResponse> responses = threadService.getThreads(chapterId, null);

        assertEquals(1, responses.size());
        assertEquals(thread.id(), responses.get(0).id());
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
        Chapter chapter = new Chapter(chapterId, manuscriptId, "Розділ 1", 1);

        when(chapterRepository.findById(chapterId)).thenReturn(Optional.of(chapter));
        when(threadRepository.findByChapterId(chapterId)).thenReturn(List.of());

        threadService.getThreads(chapterId, null);

        verify(threadRepository, times(1)).findByChapterId(chapterId);
        verify(threadRepository, never()).findByChapterIdAndStatus(any(), any());
    }

    @Test
    @DisplayName("Отримання тредів з фільтром статусу викликає findByChapterIdAndStatus")
    void getThreads_WithStatusFilter_UsesFindByChapterIdAndStatus() {
        UUID chapterId = UUID.randomUUID();
        UUID manuscriptId = UUID.randomUUID();
        Chapter chapter = new Chapter(chapterId, manuscriptId, "Розділ 1", 1);

        when(chapterRepository.findById(chapterId)).thenReturn(Optional.of(chapter));
        when(threadRepository.findByChapterIdAndStatus(chapterId, ThreadStatus.RESOLVED)).thenReturn(List.of());

        threadService.getThreads(chapterId, ThreadStatus.RESOLVED);

        verify(threadRepository, times(1)).findByChapterIdAndStatus(chapterId, ThreadStatus.RESOLVED);
        verify(threadRepository, never()).findByChapterId(any());
    }

    @Test
    @DisplayName("Успішне додавання повідомлення автором рукопису")
    void addMessage_SuccessByAuthor() {
        UUID threadId = UUID.randomUUID();
        UUID chapterId = UUID.randomUUID();
        UUID manuscriptId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        FeedbackThread thread = new FeedbackThread(threadId, chapterId, authorId, ThreadStatus.OPEN,
                false, null, null, Instant.now());
        Chapter chapter = new Chapter(chapterId, manuscriptId, "Розділ 1", 1);
        Manuscript manuscript = new Manuscript(manuscriptId, "Дюна", authorId, ManuscriptStatus.IN_PROGRESS, List.of(), "Анотація", "url", Instant.now());

        when(threadRepository.findById(threadId)).thenReturn(Optional.of(thread));
        when(chapterRepository.findById(chapterId)).thenReturn(Optional.of(chapter));
        when(manuscriptRepository.findById(manuscriptId)).thenReturn(Optional.of(manuscript));
        when(teamAssignmentRepository.findByManuscriptIdAndRole(any(), any())).thenReturn(Optional.empty());
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
        FeedbackThread thread = new FeedbackThread(threadId, chapterId, authorId, ThreadStatus.OPEN,
                false, null, null, Instant.now());
        Chapter chapter = new Chapter(chapterId, manuscriptId, "Розділ 1", 1);
        Manuscript manuscript = new Manuscript(manuscriptId, "Дюна", authorId, ManuscriptStatus.IN_PROGRESS, List.of(), "Анотація", "url", Instant.now());
        TeamAssignment editorAssignment = new TeamAssignment(UUID.randomUUID(), manuscriptId, editorId, UserRole.EDITOR, Instant.now());

        when(threadRepository.findById(threadId)).thenReturn(Optional.of(thread));
        when(chapterRepository.findById(chapterId)).thenReturn(Optional.of(chapter));
        when(manuscriptRepository.findById(manuscriptId)).thenReturn(Optional.of(manuscript));
        when(teamAssignmentRepository.findByManuscriptIdAndRole(any(), any())).thenReturn(Optional.of(editorAssignment));
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
        FeedbackThread thread = new FeedbackThread(threadId, chapterId, authorId, ThreadStatus.OPEN,
                false, null, null, Instant.now());
        Chapter chapter = new Chapter(chapterId, manuscriptId, "Розділ 1", 1);
        Manuscript manuscript = new Manuscript(manuscriptId, "Дюна", authorId, ManuscriptStatus.IN_PROGRESS, List.of(), "Анотація", "url", Instant.now());

        when(threadRepository.findById(threadId)).thenReturn(Optional.of(thread));
        when(chapterRepository.findById(chapterId)).thenReturn(Optional.of(chapter));
        when(manuscriptRepository.findById(manuscriptId)).thenReturn(Optional.of(manuscript));
        when(teamAssignmentRepository.findByManuscriptIdAndRole(any(), any())).thenReturn(Optional.empty());

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
    @DisplayName("Помилка додавання повідомлення, якщо розділ не знайдено")
    void addMessage_ChapterNotFound() {
        UUID threadId = UUID.randomUUID();
        UUID chapterId = UUID.randomUUID();
        FeedbackThread thread = new FeedbackThread(threadId, chapterId, UUID.randomUUID(), ThreadStatus.OPEN,
                false, null, null, Instant.now());

        when(threadRepository.findById(threadId)).thenReturn(Optional.of(thread));
        when(chapterRepository.findById(chapterId)).thenReturn(Optional.empty());

        assertThrows(ChapterNotFoundException.class,
                () -> threadService.addMessage(threadId, UUID.randomUUID(), new ThreadMessageRequest("Текст")));
    }

    @Test
    @DisplayName("Помилка додавання повідомлення, якщо рукопис не знайдено")
    void addMessage_ManuscriptNotFound() {
        UUID threadId = UUID.randomUUID();
        UUID chapterId = UUID.randomUUID();
        UUID manuscriptId = UUID.randomUUID();
        FeedbackThread thread = new FeedbackThread(threadId, chapterId, UUID.randomUUID(), ThreadStatus.OPEN,
                false, null, null, Instant.now());
        Chapter chapter = new Chapter(chapterId, manuscriptId, "Розділ 1", 1);

        when(threadRepository.findById(threadId)).thenReturn(Optional.of(thread));
        when(chapterRepository.findById(chapterId)).thenReturn(Optional.of(chapter));
        when(manuscriptRepository.findById(manuscriptId)).thenReturn(Optional.empty());

        assertThrows(ManuscriptNotFoundException.class,
                () -> threadService.addMessage(threadId, UUID.randomUUID(), new ThreadMessageRequest("Текст")));
    }

    @Test
    @DisplayName("Успішне прийняття пропозиції автором рукопису")
    void acceptSuggestion_Success() {
        UUID threadId = UUID.randomUUID();
        UUID chapterId = UUID.randomUUID();
        UUID manuscriptId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        FeedbackThread thread = new FeedbackThread(threadId, chapterId, UUID.randomUUID(), ThreadStatus.OPEN,
                true, "новий текст", SuggestionStatus.PENDING, Instant.now());
        Chapter chapter = new Chapter(chapterId, manuscriptId, "Розділ 1", 1);
        Manuscript manuscript = new Manuscript(manuscriptId, "Дюна", authorId, ManuscriptStatus.IN_PROGRESS, List.of(), "Анотація", "url", Instant.now());

        when(threadRepository.findById(threadId)).thenReturn(Optional.of(thread));
        when(chapterRepository.findById(chapterId)).thenReturn(Optional.of(chapter));
        when(manuscriptRepository.findById(manuscriptId)).thenReturn(Optional.of(manuscript));
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
        FeedbackThread thread = new FeedbackThread(threadId, chapterId, UUID.randomUUID(), ThreadStatus.OPEN,
                false, null, null, Instant.now());
        Chapter chapter = new Chapter(chapterId, manuscriptId, "Розділ 1", 1);
        Manuscript manuscript = new Manuscript(manuscriptId, "Дюна", authorId, ManuscriptStatus.IN_PROGRESS, List.of(), "Анотація", "url", Instant.now());

        when(threadRepository.findById(threadId)).thenReturn(Optional.of(thread));
        when(chapterRepository.findById(chapterId)).thenReturn(Optional.of(chapter));
        when(manuscriptRepository.findById(manuscriptId)).thenReturn(Optional.of(manuscript));

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
        FeedbackThread thread = new FeedbackThread(threadId, chapterId, UUID.randomUUID(), ThreadStatus.OPEN,
                true, "новий текст", SuggestionStatus.ACCEPTED, Instant.now());
        Chapter chapter = new Chapter(chapterId, manuscriptId, "Розділ 1", 1);
        Manuscript manuscript = new Manuscript(manuscriptId, "Дюна", authorId, ManuscriptStatus.IN_PROGRESS, List.of(), "Анотація", "url", Instant.now());

        when(threadRepository.findById(threadId)).thenReturn(Optional.of(thread));
        when(chapterRepository.findById(chapterId)).thenReturn(Optional.of(chapter));
        when(manuscriptRepository.findById(manuscriptId)).thenReturn(Optional.of(manuscript));

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
        FeedbackThread thread = new FeedbackThread(threadId, chapterId, UUID.randomUUID(), ThreadStatus.OPEN,
                true, "новий текст", SuggestionStatus.PENDING, Instant.now());
        Chapter chapter = new Chapter(chapterId, manuscriptId, "Розділ 1", 1);
        Manuscript manuscript = new Manuscript(manuscriptId, "Дюна", authorId, ManuscriptStatus.IN_PROGRESS, List.of(), "Анотація", "url", Instant.now());

        when(threadRepository.findById(threadId)).thenReturn(Optional.of(thread));
        when(chapterRepository.findById(chapterId)).thenReturn(Optional.of(chapter));
        when(manuscriptRepository.findById(manuscriptId)).thenReturn(Optional.of(manuscript));
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
        FeedbackThread thread = new FeedbackThread(threadId, chapterId, UUID.randomUUID(), ThreadStatus.OPEN,
                true, "новий текст", SuggestionStatus.PENDING, Instant.now());
        Chapter chapter = new Chapter(chapterId, manuscriptId, "Розділ 1", 1);
        Manuscript manuscript = new Manuscript(manuscriptId, "Дюна", UUID.randomUUID(), ManuscriptStatus.IN_PROGRESS, List.of(), "Анотація", "url", Instant.now());

        when(threadRepository.findById(threadId)).thenReturn(Optional.of(thread));
        when(chapterRepository.findById(chapterId)).thenReturn(Optional.of(chapter));
        when(manuscriptRepository.findById(manuscriptId)).thenReturn(Optional.of(manuscript));

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
        FeedbackThread thread = new FeedbackThread(threadId, chapterId, UUID.randomUUID(), ThreadStatus.OPEN,
                false, null, null, Instant.now());
        Chapter chapter = new Chapter(chapterId, manuscriptId, "Розділ 1", 1);
        Manuscript manuscript = new Manuscript(manuscriptId, "Дюна", authorId, ManuscriptStatus.IN_PROGRESS, List.of(), "Анотація", "url", Instant.now());

        when(threadRepository.findById(threadId)).thenReturn(Optional.of(thread));
        when(chapterRepository.findById(chapterId)).thenReturn(Optional.of(chapter));
        when(manuscriptRepository.findById(manuscriptId)).thenReturn(Optional.of(manuscript));

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
        FeedbackThread thread = new FeedbackThread(threadId, chapterId, UUID.randomUUID(), ThreadStatus.OPEN,
                true, "новий текст", SuggestionStatus.REJECTED, Instant.now());
        Chapter chapter = new Chapter(chapterId, manuscriptId, "Розділ 1", 1);
        Manuscript manuscript = new Manuscript(manuscriptId, "Дюна", authorId, ManuscriptStatus.IN_PROGRESS, List.of(), "Анотація", "url", Instant.now());

        when(threadRepository.findById(threadId)).thenReturn(Optional.of(thread));
        when(chapterRepository.findById(chapterId)).thenReturn(Optional.of(chapter));
        when(manuscriptRepository.findById(manuscriptId)).thenReturn(Optional.of(manuscript));

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
        FeedbackThread thread = new FeedbackThread(threadId, chapterId, authorId, ThreadStatus.OPEN,
                false, null, null, Instant.now());
        Chapter chapter = new Chapter(chapterId, manuscriptId, "Розділ 1", 1);
        Manuscript manuscript = new Manuscript(manuscriptId, "Дюна", authorId, ManuscriptStatus.IN_PROGRESS, List.of(), "Анотація", "url", Instant.now());

        when(threadRepository.findById(threadId)).thenReturn(Optional.of(thread));
        when(chapterRepository.findById(chapterId)).thenReturn(Optional.of(chapter));
        when(manuscriptRepository.findById(manuscriptId)).thenReturn(Optional.of(manuscript));
        when(teamAssignmentRepository.findByManuscriptIdAndRole(any(), any())).thenReturn(Optional.empty());
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
        FeedbackThread thread = new FeedbackThread(threadId, chapterId, authorId, ThreadStatus.OPEN,
                false, null, null, Instant.now());
        Chapter chapter = new Chapter(chapterId, manuscriptId, "Розділ 1", 1);
        Manuscript manuscript = new Manuscript(manuscriptId, "Дюна", authorId, ManuscriptStatus.IN_PROGRESS, List.of(), "Анотація", "url", Instant.now());

        when(threadRepository.findById(threadId)).thenReturn(Optional.of(thread));
        when(chapterRepository.findById(chapterId)).thenReturn(Optional.of(chapter));
        when(manuscriptRepository.findById(manuscriptId)).thenReturn(Optional.of(manuscript));
        when(teamAssignmentRepository.findByManuscriptIdAndRole(any(), any())).thenReturn(Optional.empty());

        assertThrows(ForbiddenActionException.class, () -> threadService.resolveThread(threadId, UUID.randomUUID()));
        verify(threadRepository, never()).save(any());
    }
}
