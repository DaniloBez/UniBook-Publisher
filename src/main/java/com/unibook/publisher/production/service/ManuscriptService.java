package com.unibook.publisher.production.service;

import com.unibook.publisher.common.enums.UserRole;
import com.unibook.publisher.common.event.*;
import com.unibook.publisher.common.exception.notfound.ManuscriptNotFoundException;
import com.unibook.publisher.common.exception.state.InvalidStateTransitionException;
import com.unibook.publisher.production.entity.Manuscript;
import com.unibook.publisher.production.enums.ManuscriptStatus;
import com.unibook.publisher.production.entity.request.ManuscriptApprovalRequest;
import com.unibook.publisher.production.entity.request.ManuscriptPostponementRequest;
import com.unibook.publisher.production.entity.request.ManuscriptRejectionRequest;
import com.unibook.publisher.production.entity.request.ManuscriptSubmissionRequest;
import com.unibook.publisher.production.entity.response.ManuscriptResponse;
import com.unibook.publisher.production.repository.ManuscriptRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class ManuscriptService {
    private final ManuscriptRepository manuscriptRepository;
    private final TeamAssignmentService teamAssignmentService;
    private final ApplicationEventPublisher publisher;

    public ManuscriptService(ManuscriptRepository manuscriptRepository, TeamAssignmentService teamAssignmentService, ApplicationEventPublisher publisher) {
        this.manuscriptRepository = manuscriptRepository;
        this.teamAssignmentService = teamAssignmentService;
        this.publisher = publisher;
    }

    public ManuscriptResponse getManuscriptById(UUID id) {
        Manuscript manuscript = manuscriptRepository.findById(id)
                .orElseThrow(() -> new ManuscriptNotFoundException(id));
        return ManuscriptResponse.from(manuscript);
    }

    public List<ManuscriptResponse> getManuscripts(ManuscriptStatus status) {
        List<Manuscript> manuscripts = (status == null) ? manuscriptRepository.findAll() : manuscriptRepository.findByStatus(status);
        return manuscripts.stream().map(ManuscriptResponse::from).toList();
    }

    public ManuscriptResponse approveManuscript(UUID id, UUID chiefEditorId, ManuscriptApprovalRequest request) {
        Manuscript manuscript = manuscriptRepository.findById(id)
                .orElseThrow(() -> new ManuscriptNotFoundException(id));
        if (!manuscript.status().canTransitionTo(ManuscriptStatus.IN_PROGRESS)) {
            throw new InvalidStateTransitionException(
                    "Manuscript",
                    id,
                    manuscript.status(),
                    ManuscriptStatus.IN_PROGRESS,
                    manuscript.status().allowedTransitions()
            );
        }

        teamAssignmentService.assign(id, request.editorId(), UserRole.EDITOR);
        Manuscript updated = manuscript.withStatus(ManuscriptStatus.IN_PROGRESS);
        manuscriptRepository.save(updated);

        publisher.publishEvent(new ManuscriptApprovedEvent(
                updated.manuscriptId(),
                updated.title(),
                chiefEditorId,
                updated.authorId()));

        publisher.publishEvent(new WorkerAssignedEvent(
                updated.manuscriptId(),
                updated.title(),
                chiefEditorId,
                request.editorId(),
                UserRole.EDITOR,
                updated.authorId()));

        return ManuscriptResponse.from(updated);
    }

    public ManuscriptResponse rejectManuscript(UUID id, UUID chiefEditorId, ManuscriptRejectionRequest request) {
        Manuscript manuscript = manuscriptRepository.findById(id)
                .orElseThrow(() -> new ManuscriptNotFoundException(id));
        if (!manuscript.status().canTransitionTo(ManuscriptStatus.REJECTED)) {
            throw new InvalidStateTransitionException(
                    "Manuscript",
                    id,
                    manuscript.status(),
                    ManuscriptStatus.REJECTED,
                    manuscript.status().allowedTransitions()
            );
        }

        Manuscript updated = manuscript.withStatus(ManuscriptStatus.REJECTED);
        manuscriptRepository.save(updated);
        publisher.publishEvent(new ManuscriptRejectedEvent(
                updated.manuscriptId(),
                updated.title(),
                chiefEditorId,
                updated.authorId(),
                request.reason()
        ));
        return ManuscriptResponse.from(updated);
    }

    public ManuscriptResponse postponeManuscript(UUID id, UUID chiefEditorId, ManuscriptPostponementRequest request) {
        Manuscript manuscript = manuscriptRepository.findById(id)
                .orElseThrow(() -> new ManuscriptNotFoundException(id));
        if (!manuscript.status().canTransitionTo(ManuscriptStatus.POSTPONED)) {
            throw new InvalidStateTransitionException(
                    "Manuscript",
                    id,
                    manuscript.status(),
                    ManuscriptStatus.POSTPONED,
                    manuscript.status().allowedTransitions()
            );
        }

        Manuscript updated = manuscript.withStatus(ManuscriptStatus.POSTPONED);
        manuscriptRepository.save(updated);
        publisher.publishEvent(new ManuscriptPostponedEvent(
                updated.manuscriptId(),
                updated.title(),
                chiefEditorId,
                updated.authorId(),
                request.comment()
        ));
        return ManuscriptResponse.from(updated);
    }

    public ManuscriptResponse submitManuscript(UUID authorId, ManuscriptSubmissionRequest request) {
        Manuscript manuscript = new Manuscript(
                null,
                request.title(),
                authorId,
                ManuscriptStatus.SUBMITTED,
                request.genreIds(),
                request.annotation(),
                request.draftFileUrl(),
                Instant.now()
        );
        Manuscript saved = manuscriptRepository.save(manuscript);
        publisher.publishEvent(new ManuscriptSubmittedEvent(saved.manuscriptId(), saved.title(), saved.authorId()));
        return ManuscriptResponse.from(saved);
    }
}
