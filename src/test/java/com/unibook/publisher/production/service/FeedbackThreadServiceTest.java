package com.unibook.publisher.production.service;

import com.unibook.publisher.common.event.ThreadOpenedEvent;
import com.unibook.publisher.common.exception.security.ForbiddenActionException;
import com.unibook.publisher.common.exception.state.InvalidStateTransitionException;
import com.unibook.publisher.production.entity.Chapter;
import com.unibook.publisher.production.entity.FeedbackThread;
import com.unibook.publisher.production.entity.Manuscript;
import com.unibook.publisher.production.enums.ManuscriptStatus;
import com.unibook.publisher.production.entity.request.OpenThreadRequest;
import com.unibook.publisher.production.entity.response.ThreadResponse;
import com.unibook.publisher.production.enums.SuggestionStatus;
import com.unibook.publisher.production.enums.ThreadStatus;
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
    private ApplicationEventPublisher publisher;

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
}
