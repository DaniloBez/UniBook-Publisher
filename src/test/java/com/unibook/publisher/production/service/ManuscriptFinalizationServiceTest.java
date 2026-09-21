package com.unibook.publisher.production.service;

import com.unibook.publisher.common.enums.UserRole;
import com.unibook.publisher.common.event.TextFinalizedEvent;
import com.unibook.publisher.common.exception.business.MissingCoverException;
import com.unibook.publisher.common.exception.business.UnresolvedThreadsException;
import com.unibook.publisher.common.exception.state.InvalidStateTransitionException;
import com.unibook.publisher.common.logging.AppLogger;
import com.unibook.publisher.production.entity.Chapter;
import com.unibook.publisher.production.entity.FeedbackThread;
import com.unibook.publisher.production.entity.Manuscript;
import com.unibook.publisher.production.enums.ManuscriptStatus;
import com.unibook.publisher.production.entity.TeamAssignment;
import com.unibook.publisher.production.entity.response.ManuscriptResponse;
import com.unibook.publisher.production.enums.ThreadStatus;
import com.unibook.publisher.production.repository.ChapterRepository;
import com.unibook.publisher.production.repository.CoverVersionRepository;
import com.unibook.publisher.production.repository.FeedbackThreadRepository;
import com.unibook.publisher.production.repository.ManuscriptRepository;
import com.unibook.publisher.production.repository.TeamAssignmentRepository;
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
public class ManuscriptFinalizationServiceTest {

    @Mock
    private ManuscriptRepository manuscriptRepository;

    @Mock
    private ChapterRepository chapterRepository;

    @Mock
    private FeedbackThreadRepository threadRepository;

    @Mock
    private TeamAssignmentRepository teamAssignmentRepository;

    @Mock
    private TeamAssignmentService teamAssignmentService;

    @Mock
    private CoverVersionRepository coverVersionRepository;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private ApplicationEventPublisher publisher;

    @Mock
    private AppLogger logger;

    @InjectMocks
    private ManuscriptFinalizationService finalizationService;

    private Manuscript manuscript(UUID manuscriptId, ManuscriptStatus status) {
        return new Manuscript(manuscriptId, "Дюна", UUID.randomUUID(), status, List.of(), "Анотація", "url", Instant.now());
    }

    @Test
    @DisplayName("Успішна фіналізація тексту призначеним редактором без відкритих тредів")
    void finalizeText_Success() {
        UUID manuscriptId = UUID.randomUUID();
        UUID editorId = UUID.randomUUID();
        Manuscript manuscript = manuscript(manuscriptId, ManuscriptStatus.IN_PROGRESS);
        TeamAssignment editorAssignment = new TeamAssignment(UUID.randomUUID(), manuscriptId, editorId, UserRole.EDITOR, Instant.now());

        when(manuscriptRepository.findById(manuscriptId)).thenReturn(Optional.of(manuscript));
        when(teamAssignmentRepository.findByManuscriptIdAndRole(manuscriptId, UserRole.EDITOR)).thenReturn(Optional.of(editorAssignment));
        when(chapterRepository.findByManuscriptId(manuscriptId)).thenReturn(List.of());
        when(manuscriptRepository.save(any(Manuscript.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ManuscriptResponse response = finalizationService.finalizeText(manuscriptId, editorId);

        assertEquals(ManuscriptStatus.TEXT_APPROVED, response.status());
        verify(auditLogService, times(1)).record(manuscriptId, editorId, ManuscriptStatus.IN_PROGRESS, ManuscriptStatus.TEXT_APPROVED);
        verify(publisher, times(1)).publishEvent(any(TextFinalizedEvent.class));
    }

    @Test
    @DisplayName("Заборонено фіналізувати текст за наявності відкритого треду")
    void finalizeText_InvalidStateWhenOpenThreadsExist() {
        UUID manuscriptId = UUID.randomUUID();
        UUID editorId = UUID.randomUUID();
        UUID chapterId = UUID.randomUUID();
        Manuscript manuscript = manuscript(manuscriptId, ManuscriptStatus.IN_PROGRESS);
        TeamAssignment editorAssignment = new TeamAssignment(UUID.randomUUID(), manuscriptId, editorId, UserRole.EDITOR, Instant.now());
        Chapter chapter = new Chapter(chapterId, manuscriptId, "Розділ 1", 1);
        FeedbackThread openThread = new FeedbackThread(UUID.randomUUID(), chapterId, UUID.randomUUID(),
                ThreadStatus.OPEN, false, null, null, Instant.now());

        when(manuscriptRepository.findById(manuscriptId)).thenReturn(Optional.of(manuscript));
        when(teamAssignmentRepository.findByManuscriptIdAndRole(manuscriptId, UserRole.EDITOR)).thenReturn(Optional.of(editorAssignment));
        when(chapterRepository.findByManuscriptId(manuscriptId)).thenReturn(List.of(chapter));
        when(threadRepository.findByChapterId(chapterId)).thenReturn(List.of(openThread));

        assertThrows(UnresolvedThreadsException.class, () -> finalizationService.finalizeText(manuscriptId, editorId));
        verify(manuscriptRepository, never()).save(any());
    }

    @Test
    @DisplayName("Заборонено публікацію без жодної версії обкладинки")
    void publish_InvalidStateWhenNoCoverVersion() {
        UUID manuscriptId = UUID.randomUUID();
        UUID chiefEditorId = UUID.randomUUID();
        Manuscript manuscript = manuscript(manuscriptId, ManuscriptStatus.IN_DESIGN);

        when(manuscriptRepository.findById(manuscriptId)).thenReturn(Optional.of(manuscript));
        when(coverVersionRepository.findByManuscriptId(manuscriptId)).thenReturn(List.of());

        assertThrows(MissingCoverException.class, () -> finalizationService.publish(manuscriptId, chiefEditorId));
        verify(manuscriptRepository, never()).save(any());
    }
}
