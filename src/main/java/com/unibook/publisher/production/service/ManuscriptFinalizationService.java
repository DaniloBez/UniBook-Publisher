package com.unibook.publisher.production.service;

import com.unibook.publisher.common.enums.UserRole;
import com.unibook.publisher.common.event.ManuscriptPublishedEvent;
import com.unibook.publisher.common.event.TextFinalizedEvent;
import com.unibook.publisher.common.event.WorkerAssignedEvent;
import com.unibook.publisher.common.exception.ForbiddenActionException;
import com.unibook.publisher.common.exception.InvalidStateTransitionException;
import com.unibook.publisher.common.exception.ResourceNotFoundException;
import com.unibook.publisher.production.entity.Chapter;
import com.unibook.publisher.production.entity.Manuscript;
import com.unibook.publisher.production.entity.ManuscriptStatus;
import com.unibook.publisher.production.entity.TeamAssignment;
import com.unibook.publisher.production.entity.request.AssignDesignerRequest;
import com.unibook.publisher.production.entity.response.AuditLogResponse;
import com.unibook.publisher.production.entity.response.ManuscriptResponse;
import com.unibook.publisher.production.enums.SuggestionStatus;
import com.unibook.publisher.production.enums.ThreadStatus;
import com.unibook.publisher.production.repository.ChapterRepository;
import com.unibook.publisher.production.repository.CoverVersionRepository;
import com.unibook.publisher.production.repository.FeedbackThreadRepository;
import com.unibook.publisher.production.repository.ManuscriptRepository;
import com.unibook.publisher.production.repository.TeamAssignmentRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class ManuscriptFinalizationService {
    private final ManuscriptRepository manuscriptRepository;
    private final ChapterRepository chapterRepository;
    private final FeedbackThreadRepository threadRepository;
    private final TeamAssignmentRepository teamAssignmentRepository;
    private final TeamAssignmentService teamAssignmentService;
    private final CoverVersionRepository coverVersionRepository;
    private final AuditLogService auditLogService;
    private final ApplicationEventPublisher publisher;

    public ManuscriptFinalizationService(
            ManuscriptRepository manuscriptRepository,
            ChapterRepository chapterRepository,
            FeedbackThreadRepository threadRepository,
            TeamAssignmentRepository teamAssignmentRepository,
            TeamAssignmentService teamAssignmentService,
            CoverVersionRepository coverVersionRepository,
            AuditLogService auditLogService,
            ApplicationEventPublisher publisher
    ) {
        this.manuscriptRepository = manuscriptRepository;
        this.chapterRepository = chapterRepository;
        this.threadRepository = threadRepository;
        this.teamAssignmentRepository = teamAssignmentRepository;
        this.teamAssignmentService = teamAssignmentService;
        this.coverVersionRepository = coverVersionRepository;
        this.auditLogService = auditLogService;
        this.publisher = publisher;
    }

    public ManuscriptResponse finalizeText(UUID manuscriptId, UUID editorId) {
        Manuscript manuscript = manuscriptRepository.findById(manuscriptId)
                .orElseThrow(() -> new ResourceNotFoundException("Рукопис за ID: " + manuscriptId + " не знайдено"));

        TeamAssignment editorAssignment = teamAssignmentRepository.findByManuscriptIdAndRole(manuscriptId, UserRole.EDITOR)
                .orElseThrow(() -> new ForbiddenActionException("Редактора не призначено на цей рукопис"));
        if (!editorAssignment.userId().equals(editorId)) {
            throw new ForbiddenActionException("Фіналізувати текст може лише призначений редактор");
        }
        if (manuscript.status() != ManuscriptStatus.IN_PROGRESS) {
            throw new InvalidStateTransitionException("Текст можна фіналізувати лише зі статусу IN_PROGRESS. Поточний статус: " + manuscript.status());
        }

        boolean hasOpenWork = chapterRepository.findByManuscriptId(manuscriptId).stream()
                .map(Chapter::chapterId)
                .flatMap(chapterId -> threadRepository.findByChapterId(chapterId).stream())
                .anyMatch(thread -> thread.status() == ThreadStatus.OPEN
                        || (thread.isSuggestion() && thread.suggestionStatus() == SuggestionStatus.PENDING));
        if (hasOpenWork) {
            throw new InvalidStateTransitionException("Є незакриті треди або необроблені пропозиції - фіналізація неможлива");
        }

        Manuscript updated = manuscript.withStatus(ManuscriptStatus.TEXT_APPROVED);
        manuscriptRepository.save(updated);
        auditLogService.record(manuscriptId, editorId, manuscript.status(), updated.status());

        publisher.publishEvent(new TextFinalizedEvent(manuscriptId, updated.title(), editorId, updated.authorId()));
        return ManuscriptResponse.from(updated);
    }

    public ManuscriptResponse assignDesigner(UUID manuscriptId, UUID chiefEditorId, AssignDesignerRequest request) {
        Manuscript manuscript = manuscriptRepository.findById(manuscriptId)
                .orElseThrow(() -> new ResourceNotFoundException("Рукопис за ID: " + manuscriptId + " не знайдено"));
        if (manuscript.status() != ManuscriptStatus.TEXT_APPROVED) {
            throw new InvalidStateTransitionException("Призначити дизайнера можна лише після фіналізації тексту (TEXT_APPROVED)");
        }

        teamAssignmentService.assign(manuscriptId, request.designerId(), UserRole.DESIGNER);

        Manuscript updated = manuscript.withStatus(ManuscriptStatus.IN_DESIGN);
        manuscriptRepository.save(updated);
        auditLogService.record(manuscriptId, chiefEditorId, manuscript.status(), updated.status());

        publisher.publishEvent(new WorkerAssignedEvent(
                manuscriptId, updated.title(), chiefEditorId, request.designerId(), UserRole.DESIGNER, updated.authorId()
        ));
        return ManuscriptResponse.from(updated);
    }

    public ManuscriptResponse publish(UUID manuscriptId, UUID chiefEditorId) {
        Manuscript manuscript = manuscriptRepository.findById(manuscriptId)
                .orElseThrow(() -> new ResourceNotFoundException("Рукопис за ID: " + manuscriptId + " не знайдено"));

        if (manuscript.status() != ManuscriptStatus.IN_DESIGN) {
            throw new InvalidStateTransitionException("Публікація можлива лише зі статусу IN_DESIGN. Поточний статус: " + manuscript.status());
        }

        if (coverVersionRepository.findByManuscriptId(manuscriptId).isEmpty()) {
            throw new InvalidStateTransitionException("Публікація неможлива без жодної версії обкладинки");
        }

        Manuscript updated = manuscript.withStatus(ManuscriptStatus.PUBLISHED);
        manuscriptRepository.save(updated);
        auditLogService.record(manuscriptId, chiefEditorId, manuscript.status(), updated.status());

        publisher.publishEvent(new ManuscriptPublishedEvent(manuscriptId, updated.title(), updated.authorId()));
        return ManuscriptResponse.from(updated);
    }

    public List<AuditLogResponse> getAuditLog(UUID manuscriptId) {
        if (manuscriptRepository.findById(manuscriptId).isEmpty()) {
            throw new ResourceNotFoundException("Рукопис за ID: " + manuscriptId + " не знайдено");
        }
        return auditLogService.getAuditLog(manuscriptId);
    }
}
