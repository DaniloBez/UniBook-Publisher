package com.unibook.publisher.production.service;

import com.unibook.publisher.common.enums.UserRole;
import com.unibook.publisher.common.event.ManuscriptPublishedEvent;
import com.unibook.publisher.common.event.TextFinalizedEvent;
import com.unibook.publisher.common.event.WorkerAssignedEvent;
import com.unibook.publisher.common.exception.business.MissingCoverException;
import com.unibook.publisher.common.exception.business.UnresolvedThreadsException;
import com.unibook.publisher.common.exception.notfound.ManuscriptNotFoundException;
import com.unibook.publisher.common.exception.security.ForbiddenActionException;
import com.unibook.publisher.common.exception.state.InvalidStateTransitionException;
import com.unibook.publisher.common.logging.AppLogger;
import com.unibook.publisher.production.entity.Chapter;
import com.unibook.publisher.production.entity.CoverVersion;
import com.unibook.publisher.production.entity.FeedbackThread;
import com.unibook.publisher.production.entity.Manuscript;
import com.unibook.publisher.production.enums.ManuscriptStatus;
import com.unibook.publisher.production.entity.TeamAssignment;
import com.unibook.publisher.production.entity.request.AssignDesignerRequest;
import com.unibook.publisher.production.entity.response.AuditLogResponse;
import com.unibook.publisher.production.entity.response.ManuscriptResponse;
import com.unibook.publisher.production.entity.response.TeamAssignmentResponse;
import com.unibook.publisher.production.enums.SuggestionStatus;
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
public class ManuscriptFinalizationServiceImplTest {

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
    private ManuscriptFinalizationServiceImpl finalizationService;

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

    @Test
    @DisplayName("Помилка фіналізації тексту, якщо рукопис не знайдено")
    void finalizeText_ManuscriptNotFound() {
        UUID manuscriptId = UUID.randomUUID();
        UUID editorId = UUID.randomUUID();

        when(manuscriptRepository.findById(manuscriptId)).thenReturn(Optional.empty());

        assertThrows(ManuscriptNotFoundException.class, () -> finalizationService.finalizeText(manuscriptId, editorId));
        verify(manuscriptRepository, never()).save(any());
    }

    @Test
    @DisplayName("Заборонено фіналізувати текст, якщо редактора не призначено")
    void finalizeText_ForbiddenWhenNoEditorAssigned() {
        UUID manuscriptId = UUID.randomUUID();
        UUID editorId = UUID.randomUUID();
        Manuscript manuscript = manuscript(manuscriptId, ManuscriptStatus.IN_PROGRESS);

        when(manuscriptRepository.findById(manuscriptId)).thenReturn(Optional.of(manuscript));
        when(teamAssignmentRepository.findByManuscriptIdAndRole(manuscriptId, UserRole.EDITOR)).thenReturn(Optional.empty());

        assertThrows(ForbiddenActionException.class, () -> finalizationService.finalizeText(manuscriptId, editorId));
        verify(manuscriptRepository, never()).save(any());
    }

    @Test
    @DisplayName("Заборонено фіналізувати текст користувачу, який не є призначеним редактором")
    void finalizeText_ForbiddenWhenEditorMismatch() {
        UUID manuscriptId = UUID.randomUUID();
        UUID editorId = UUID.randomUUID();
        Manuscript manuscript = manuscript(manuscriptId, ManuscriptStatus.IN_PROGRESS);
        TeamAssignment editorAssignment = new TeamAssignment(UUID.randomUUID(), manuscriptId, UUID.randomUUID(), UserRole.EDITOR, Instant.now());

        when(manuscriptRepository.findById(manuscriptId)).thenReturn(Optional.of(manuscript));
        when(teamAssignmentRepository.findByManuscriptIdAndRole(manuscriptId, UserRole.EDITOR)).thenReturn(Optional.of(editorAssignment));

        assertThrows(ForbiddenActionException.class, () -> finalizationService.finalizeText(manuscriptId, editorId));
        verify(manuscriptRepository, never()).save(any());
    }

    @Test
    @DisplayName("Заборонено фіналізувати текст, якщо перехід статусу неможливий")
    void finalizeText_InvalidStateTransition() {
        UUID manuscriptId = UUID.randomUUID();
        UUID editorId = UUID.randomUUID();
        Manuscript manuscript = manuscript(manuscriptId, ManuscriptStatus.PUBLISHED);
        TeamAssignment editorAssignment = new TeamAssignment(UUID.randomUUID(), manuscriptId, editorId, UserRole.EDITOR, Instant.now());

        when(manuscriptRepository.findById(manuscriptId)).thenReturn(Optional.of(manuscript));
        when(teamAssignmentRepository.findByManuscriptIdAndRole(manuscriptId, UserRole.EDITOR)).thenReturn(Optional.of(editorAssignment));

        assertThrows(InvalidStateTransitionException.class, () -> finalizationService.finalizeText(manuscriptId, editorId));
        verify(manuscriptRepository, never()).save(any());
    }

    @Test
    @DisplayName("Заборонено фіналізувати текст за наявності нерозглянутої пропозиції")
    void finalizeText_InvalidStateWhenPendingSuggestionExists() {
        UUID manuscriptId = UUID.randomUUID();
        UUID editorId = UUID.randomUUID();
        UUID chapterId = UUID.randomUUID();
        Manuscript manuscript = manuscript(manuscriptId, ManuscriptStatus.IN_PROGRESS);
        TeamAssignment editorAssignment = new TeamAssignment(UUID.randomUUID(), manuscriptId, editorId, UserRole.EDITOR, Instant.now());
        Chapter chapter = new Chapter(chapterId, manuscriptId, "Розділ 1", 1);
        FeedbackThread suggestionThread = new FeedbackThread(UUID.randomUUID(), chapterId, UUID.randomUUID(),
                ThreadStatus.RESOLVED, true, "новий текст", SuggestionStatus.PENDING, Instant.now());

        when(manuscriptRepository.findById(manuscriptId)).thenReturn(Optional.of(manuscript));
        when(teamAssignmentRepository.findByManuscriptIdAndRole(manuscriptId, UserRole.EDITOR)).thenReturn(Optional.of(editorAssignment));
        when(chapterRepository.findByManuscriptId(manuscriptId)).thenReturn(List.of(chapter));
        when(threadRepository.findByChapterId(chapterId)).thenReturn(List.of(suggestionThread));

        assertThrows(UnresolvedThreadsException.class, () -> finalizationService.finalizeText(manuscriptId, editorId));
        verify(manuscriptRepository, never()).save(any());
    }

    @Test
    @DisplayName("Успішне призначення дизайнера головним редактором")
    void assignDesigner_Success() {
        UUID manuscriptId = UUID.randomUUID();
        UUID chiefEditorId = UUID.randomUUID();
        UUID designerId = UUID.randomUUID();
        Manuscript manuscript = manuscript(manuscriptId, ManuscriptStatus.TEXT_APPROVED);
        AssignDesignerRequest request = new AssignDesignerRequest(designerId);

        when(manuscriptRepository.findById(manuscriptId)).thenReturn(Optional.of(manuscript));
        when(teamAssignmentService.assign(manuscriptId, designerId, UserRole.DESIGNER))
                .thenReturn(new TeamAssignmentResponse(UUID.randomUUID(), manuscriptId, designerId, UserRole.DESIGNER, Instant.now()));
        when(manuscriptRepository.save(any(Manuscript.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ManuscriptResponse response = finalizationService.assignDesigner(manuscriptId, chiefEditorId, request);

        assertEquals(ManuscriptStatus.IN_DESIGN, response.status());
        verify(teamAssignmentService, times(1)).assign(manuscriptId, designerId, UserRole.DESIGNER);
        verify(publisher, times(1)).publishEvent(any(WorkerAssignedEvent.class));
    }

    @Test
    @DisplayName("Помилка призначення дизайнера, якщо рукопис не знайдено")
    void assignDesigner_ManuscriptNotFound() {
        UUID manuscriptId = UUID.randomUUID();
        UUID chiefEditorId = UUID.randomUUID();
        AssignDesignerRequest request = new AssignDesignerRequest(UUID.randomUUID());

        when(manuscriptRepository.findById(manuscriptId)).thenReturn(Optional.empty());

        assertThrows(ManuscriptNotFoundException.class, () -> finalizationService.assignDesigner(manuscriptId, chiefEditorId, request));
        verify(teamAssignmentService, never()).assign(any(), any(), any());
    }

    @Test
    @DisplayName("Заборонено призначати дизайнера, якщо перехід статусу неможливий")
    void assignDesigner_InvalidStateTransition() {
        UUID manuscriptId = UUID.randomUUID();
        UUID chiefEditorId = UUID.randomUUID();
        Manuscript manuscript = manuscript(manuscriptId, ManuscriptStatus.IN_PROGRESS);
        AssignDesignerRequest request = new AssignDesignerRequest(UUID.randomUUID());

        when(manuscriptRepository.findById(manuscriptId)).thenReturn(Optional.of(manuscript));

        assertThrows(InvalidStateTransitionException.class, () -> finalizationService.assignDesigner(manuscriptId, chiefEditorId, request));
        verify(teamAssignmentService, never()).assign(any(), any(), any());
        verify(manuscriptRepository, never()).save(any());
    }

    @Test
    @DisplayName("Успішна публікація рукопису")
    void publish_Success() {
        UUID manuscriptId = UUID.randomUUID();
        UUID chiefEditorId = UUID.randomUUID();
        Manuscript manuscript = manuscript(manuscriptId, ManuscriptStatus.IN_DESIGN);
        CoverVersion cover = new CoverVersion(UUID.randomUUID(), manuscriptId, "url", UUID.randomUUID(), 1, Instant.now());

        when(manuscriptRepository.findById(manuscriptId)).thenReturn(Optional.of(manuscript));
        when(coverVersionRepository.findByManuscriptId(manuscriptId)).thenReturn(List.of(cover));
        when(manuscriptRepository.save(any(Manuscript.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ManuscriptResponse response = finalizationService.publish(manuscriptId, chiefEditorId);

        assertEquals(ManuscriptStatus.PUBLISHED, response.status());
        verify(publisher, times(1)).publishEvent(any(ManuscriptPublishedEvent.class));
    }

    @Test
    @DisplayName("Помилка публікації, якщо рукопис не знайдено")
    void publish_ManuscriptNotFound() {
        UUID manuscriptId = UUID.randomUUID();
        UUID chiefEditorId = UUID.randomUUID();

        when(manuscriptRepository.findById(manuscriptId)).thenReturn(Optional.empty());

        assertThrows(ManuscriptNotFoundException.class, () -> finalizationService.publish(manuscriptId, chiefEditorId));
        verify(manuscriptRepository, never()).save(any());
    }

    @Test
    @DisplayName("Заборонено публікацію, якщо перехід статусу неможливий")
    void publish_InvalidStateTransition() {
        UUID manuscriptId = UUID.randomUUID();
        UUID chiefEditorId = UUID.randomUUID();
        Manuscript manuscript = manuscript(manuscriptId, ManuscriptStatus.IN_PROGRESS);

        when(manuscriptRepository.findById(manuscriptId)).thenReturn(Optional.of(manuscript));

        assertThrows(InvalidStateTransitionException.class, () -> finalizationService.publish(manuscriptId, chiefEditorId));
        verify(manuscriptRepository, never()).save(any());
    }

    @Test
    @DisplayName("Успішне отримання журналу аудиту рукопису")
    void getAuditLog_Success() {
        UUID manuscriptId = UUID.randomUUID();
        Manuscript manuscript = manuscript(manuscriptId, ManuscriptStatus.IN_PROGRESS);
        List<AuditLogResponse> expected = List.of(new AuditLogResponse(
                UUID.randomUUID(), manuscriptId, UUID.randomUUID(), ManuscriptStatus.SUBMITTED, ManuscriptStatus.IN_PROGRESS, Instant.now()));

        when(manuscriptRepository.findById(manuscriptId)).thenReturn(Optional.of(manuscript));
        when(auditLogService.getAuditLog(manuscriptId)).thenReturn(expected);

        List<AuditLogResponse> result = finalizationService.getAuditLog(manuscriptId);

        assertEquals(expected, result);
        verify(auditLogService, times(1)).getAuditLog(manuscriptId);
    }

    @Test
    @DisplayName("Помилка отримання журналу аудиту, якщо рукопис не знайдено")
    void getAuditLog_ManuscriptNotFound() {
        UUID manuscriptId = UUID.randomUUID();

        when(manuscriptRepository.findById(manuscriptId)).thenReturn(Optional.empty());

        assertThrows(ManuscriptNotFoundException.class, () -> finalizationService.getAuditLog(manuscriptId));
        verify(auditLogService, never()).getAuditLog(any());
    }
}
