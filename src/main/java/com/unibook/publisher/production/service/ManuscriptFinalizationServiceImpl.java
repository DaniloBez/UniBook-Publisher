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
import com.unibook.publisher.production.entity.Manuscript;
import com.unibook.publisher.production.enums.ManuscriptStatus;
import com.unibook.publisher.production.entity.TeamAssignment;
import com.unibook.publisher.production.entity.request.AssignDesignerRequest;
import com.unibook.publisher.production.entity.response.ManuscriptAuditLogResponse;
import com.unibook.publisher.production.entity.response.ManuscriptResponse;
import com.unibook.publisher.production.enums.SuggestionStatus;
import com.unibook.publisher.production.enums.ThreadStatus;
import com.unibook.publisher.production.repository.CoverVersionRepository;
import com.unibook.publisher.production.repository.FeedbackThreadRepository;
import com.unibook.publisher.production.repository.ManuscriptRepository;
import com.unibook.publisher.production.repository.TeamAssignmentRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ManuscriptFinalizationServiceImpl implements ManuscriptFinalizationService {
    private final ManuscriptRepository manuscriptRepository;
    private final FeedbackThreadRepository threadRepository;
    private final TeamAssignmentRepository teamAssignmentRepository;
    private final TeamAssignmentService teamAssignmentService;
    private final CoverVersionRepository coverVersionRepository;
    private final ManuscriptAuditLogService manuscriptAuditLogService;
    private final ApplicationEventPublisher publisher;
    private final AppLogger logger;

    public ManuscriptFinalizationServiceImpl(
            ManuscriptRepository manuscriptRepository,
            FeedbackThreadRepository threadRepository,
            TeamAssignmentRepository teamAssignmentRepository,
            TeamAssignmentService teamAssignmentService,
            CoverVersionRepository coverVersionRepository,
            ManuscriptAuditLogService manuscriptAuditLogService,
            ApplicationEventPublisher publisher,
            AppLogger logger
    ) {
        this.manuscriptRepository = manuscriptRepository;
        this.threadRepository = threadRepository;
        this.teamAssignmentRepository = teamAssignmentRepository;
        this.teamAssignmentService = teamAssignmentService;
        this.coverVersionRepository = coverVersionRepository;
        this.manuscriptAuditLogService = manuscriptAuditLogService;
        this.publisher = publisher;
        this.logger = logger;
    }

    @Override
    @Transactional
    public ManuscriptResponse finalizeText(UUID manuscriptId, UUID editorId) {
        Manuscript manuscript = manuscriptRepository.findById(manuscriptId)
                .orElseThrow(() -> new ManuscriptNotFoundException(manuscriptId));
        TeamAssignment editorAssignment = teamAssignmentRepository.findByManuscript_ManuscriptIdAndRole(manuscriptId, UserRole.EDITOR)
                .orElseThrow(() -> new ForbiddenActionException("Редактора не призначено на цей рукопис"));

        if (!editorAssignment.getUserId().equals(editorId))
            throw new ForbiddenActionException("Фіналізувати текст може лише призначений редактор");
        if (!manuscript.getStatus().canTransitionTo(ManuscriptStatus.TEXT_APPROVED)) {
            throw new InvalidStateTransitionException(
                    "Manuscript",
                    manuscriptId,
                    manuscript.getStatus(),
                    ManuscriptStatus.TEXT_APPROVED,
                    manuscript.getStatus().allowedTransitions()
            );
        }

        boolean hasOpenWork = threadRepository.findByChapter_Manuscript_ManuscriptId(manuscriptId).stream()
                .anyMatch(thread -> thread.getStatus() == ThreadStatus.OPEN
                        || (thread.isSuggestion() && thread.getSuggestionStatus() == SuggestionStatus.PENDING));

        if (hasOpenWork) throw new UnresolvedThreadsException();

        ManuscriptStatus oldStatus = manuscript.getStatus();
        manuscript.setStatus(ManuscriptStatus.TEXT_APPROVED);
        Manuscript updated = manuscriptRepository.save(manuscript);

        logger.info(
            "Updated manuscript {} status: {} -> {} by editor {}",
            manuscriptId,
            oldStatus,
            updated.getStatus(),
            editorId
        );
        manuscriptAuditLogService.recordStatusChange(manuscriptId, editorId, oldStatus, updated.getStatus());

        publisher.publishEvent(new TextFinalizedEvent(
                manuscriptId,
                updated.getTitle(),
                editorId,
                updated.getAuthorId()
        ));
        return ManuscriptResponse.from(updated);
    }

    @Override
    @Transactional
    public ManuscriptResponse assignDesigner(UUID manuscriptId, UUID chiefEditorId, AssignDesignerRequest request) {
        Manuscript manuscript = manuscriptRepository.findById(manuscriptId)
                .orElseThrow(() -> new ManuscriptNotFoundException(manuscriptId));
        if (!manuscript.getStatus().canTransitionTo(ManuscriptStatus.IN_DESIGN)) {
            throw new InvalidStateTransitionException(
                    "Manuscript",
                    manuscriptId,
                    manuscript.getStatus(),
                    ManuscriptStatus.IN_DESIGN,
                    manuscript.getStatus().allowedTransitions()
            );
        }

        teamAssignmentService.assign(manuscriptId, request.designerId(), UserRole.DESIGNER);

        ManuscriptStatus oldStatus = manuscript.getStatus();
        manuscript.setStatus(ManuscriptStatus.IN_DESIGN);
        Manuscript updated = manuscriptRepository.save(manuscript);

        logger.info(
            "Updated manuscript {} status: {} -> {} and assigned designer {} by chief editor {}",
            manuscriptId,
            oldStatus,
            updated.getStatus(),
            request.designerId(),
            chiefEditorId
        );
        manuscriptAuditLogService.recordStatusChange(manuscriptId, chiefEditorId, oldStatus, updated.getStatus());

        publisher.publishEvent(new WorkerAssignedEvent(
                manuscriptId,
                updated.getTitle(),
                chiefEditorId,
                request.designerId(),
                UserRole.DESIGNER,
                updated.getAuthorId()
        ));
        return ManuscriptResponse.from(updated);
    }

    @Override
    @Transactional
    public ManuscriptResponse publish(UUID manuscriptId, UUID chiefEditorId) {
        Manuscript manuscript = manuscriptRepository.findById(manuscriptId)
                .orElseThrow(() -> new ManuscriptNotFoundException(manuscriptId));

        if (!manuscript.getStatus().canTransitionTo(ManuscriptStatus.PUBLISHED)) {
            throw new InvalidStateTransitionException(
                    "Manuscript",
                    manuscriptId,
                    manuscript.getStatus(),
                    ManuscriptStatus.PUBLISHED,
                    manuscript.getStatus().allowedTransitions()
            );
        }
        if (!coverVersionRepository.existsByManuscript_ManuscriptId(manuscriptId)) throw new MissingCoverException();

        ManuscriptStatus oldStatus = manuscript.getStatus();
        manuscript.setStatus(ManuscriptStatus.PUBLISHED);
        Manuscript updated = manuscriptRepository.save(manuscript);

        logger.info(
            "Updated manuscript {} status: {} -> PUBLISHED by chief editor {}",
            manuscriptId,
            oldStatus,
            chiefEditorId
        );
        manuscriptAuditLogService.recordStatusChange(manuscriptId, chiefEditorId, oldStatus, updated.getStatus());

        publisher.publishEvent(new ManuscriptPublishedEvent(
                manuscriptId,
                updated.getTitle(),
                updated.getAuthorId()
        ));
        return ManuscriptResponse.from(updated);
    }

    @Override
    public List<ManuscriptAuditLogResponse> getAuditLog(UUID manuscriptId) {
        if (manuscriptRepository.findById(manuscriptId).isEmpty())
            throw new ManuscriptNotFoundException(manuscriptId);

        return manuscriptAuditLogService.getAuditLog(manuscriptId);
    }
}
