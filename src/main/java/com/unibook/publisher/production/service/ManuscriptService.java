package com.unibook.publisher.production.service;

import com.unibook.publisher.common.enums.UserRole;
import com.unibook.publisher.common.event.ManuscriptApprovedEvent;
import com.unibook.publisher.common.event.ManuscriptPostponedEvent;
import com.unibook.publisher.common.event.ManuscriptRejectedEvent;
import com.unibook.publisher.common.event.ManuscriptSubmittedEvent;
import com.unibook.publisher.common.exception.ResourceNotFoundException;
import com.unibook.publisher.production.entity.Manuscript;
import com.unibook.publisher.production.entity.ManuscriptStatus;
import com.unibook.publisher.production.entity.TeamAssignment;
import com.unibook.publisher.production.entity.request.ManuscriptApprovalRequest;
import com.unibook.publisher.production.entity.request.ManuscriptPostponementRequest;
import com.unibook.publisher.production.entity.request.ManuscriptRejectionRequest;
import com.unibook.publisher.production.entity.request.ManuscriptSubmissionRequest;
import com.unibook.publisher.production.entity.response.ManuscriptResponse;
import com.unibook.publisher.production.repository.ManuscriptRepository;
import com.unibook.publisher.production.repository.TeamAssignmentRepository;
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
                .orElseThrow(() -> new ResourceNotFoundException("Рукопис за ID: " + id +" не знайдено"));
        return ManuscriptResponse.from(manuscript);
    }

    public List<ManuscriptResponse> getAllManuscripts() {
        return manuscriptRepository.findAll().stream()
                .map(ManuscriptResponse::from)
                .toList();
    }

    public ManuscriptResponse approveManuscript(UUID id, ManuscriptApprovalRequest request) {
        Manuscript manuscript = manuscriptRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Рукопис за ID: " + id +" не знайдено"));

        teamAssignmentService.assignUser(id, request.editorId(), UserRole.EDITOR);

        Manuscript updated = manuscript.withStatus(ManuscriptStatus.IN_PROGRESS);
        manuscriptRepository.save(updated);
        publisher.publishEvent(new ManuscriptApprovedEvent(
                updated.manuscriptId(),
                updated.title(),
                request.editorId(),
                updated.authorId()));
        return ManuscriptResponse.from(updated);
    }

    public ManuscriptResponse rejectManuscript(UUID id, UUID chiefEditorId, ManuscriptRejectionRequest request) {
        Manuscript manuscript = manuscriptRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Рукопис за ID: " + id +" не знайдено"));
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
                .orElseThrow(() -> new ResourceNotFoundException("Рукопис за ID: " + id +" не знайдено"));
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
