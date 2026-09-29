package com.unibook.publisher.production.service;

import com.unibook.publisher.common.enums.UserRole;
import com.unibook.publisher.common.event.*;
import com.unibook.publisher.common.exception.notfound.ManuscriptNotFoundException;
import com.unibook.publisher.common.exception.state.InvalidStateTransitionException;
import com.unibook.publisher.common.logging.AppLogger;
import com.unibook.publisher.production.entity.Genre;
import com.unibook.publisher.production.entity.Manuscript;
import com.unibook.publisher.production.enums.ManuscriptStatus;
import com.unibook.publisher.production.entity.request.ManuscriptApprovalRequest;
import com.unibook.publisher.production.entity.request.ManuscriptPostponementRequest;
import com.unibook.publisher.production.entity.request.ManuscriptRejectionRequest;
import com.unibook.publisher.production.entity.request.ManuscriptSubmissionRequest;
import com.unibook.publisher.production.entity.response.ManuscriptResponse;
import com.unibook.publisher.production.repository.GenreRepository;
import com.unibook.publisher.production.repository.ManuscriptRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ManuscriptServiceImpl implements ManuscriptService {

    private final ManuscriptRepository manuscriptRepository;
    private final GenreRepository genreRepository;
    private final TeamAssignmentService teamAssignmentService;
    private final ApplicationEventPublisher publisher;
    private final AppLogger logger;

    public ManuscriptServiceImpl(ManuscriptRepository manuscriptRepository, GenreRepository genreRepository, TeamAssignmentService teamAssignmentService, ApplicationEventPublisher publisher, AppLogger logger) {
        this.manuscriptRepository = manuscriptRepository;
        this.genreRepository = genreRepository;
        this.teamAssignmentService = teamAssignmentService;
        this.publisher = publisher;
        this.logger = logger;
    }

    @Override
    public ManuscriptResponse getManuscriptById(UUID id) {
        Manuscript manuscript = manuscriptRepository.findByIdWithGenres(id)
                .orElseThrow(() -> new ManuscriptNotFoundException(id));
        return ManuscriptResponse.from(manuscript);
    }

    @Override
    public List<ManuscriptResponse> getManuscripts(ManuscriptStatus status) {
        List<Manuscript> manuscripts = (status == null) ? manuscriptRepository.findAllWithGenres() : manuscriptRepository.findByStatusWithGenres(status);
        return manuscripts.stream().map(ManuscriptResponse::from).toList();
    }

    @Override
    @Transactional
    public ManuscriptResponse approveManuscript(UUID id, UUID chiefEditorId, ManuscriptApprovalRequest request) {
        Manuscript manuscript = manuscriptRepository.findByIdWithGenres(id)
                .orElseThrow(() -> new ManuscriptNotFoundException(id));
        if (!manuscript.getStatus().canTransitionTo(ManuscriptStatus.IN_PROGRESS)) {
            throw new InvalidStateTransitionException(
                    "Manuscript",
                    id,
                    manuscript.getStatus(),
                    ManuscriptStatus.IN_PROGRESS,
                    manuscript.getStatus().allowedTransitions()
            );
        }

        ManuscriptStatus oldStatus = manuscript.getStatus();
        teamAssignmentService.assign(id, request.editorId(), UserRole.EDITOR);
        manuscript.setStatus(ManuscriptStatus.IN_PROGRESS);
        Manuscript updated = manuscriptRepository.save(manuscript);

        logger.info(
                "Updated manuscript {} status {} -> IN_PROGRESS by chief editor {}",
                updated.getManuscriptId(),
                oldStatus,
                chiefEditorId
        );

        publisher.publishEvent(new ManuscriptApprovedEvent(
                updated.getManuscriptId(),
                updated.getTitle(),
                chiefEditorId,
                updated.getAuthorId()));

        publisher.publishEvent(new WorkerAssignedEvent(
                updated.getManuscriptId(),
                updated.getTitle(),
                chiefEditorId,
                request.editorId(),
                UserRole.EDITOR,
                updated.getAuthorId()));
        return ManuscriptResponse.from(updated);
    }

    @Override
    @Transactional
    public ManuscriptResponse rejectManuscript(UUID id, UUID chiefEditorId, ManuscriptRejectionRequest request) {
        Manuscript manuscript = manuscriptRepository.findByIdWithGenres(id)
                .orElseThrow(() -> new ManuscriptNotFoundException(id));
        if (!manuscript.getStatus().canTransitionTo(ManuscriptStatus.REJECTED)) {
            throw new InvalidStateTransitionException(
                    "Manuscript",
                    id,
                    manuscript.getStatus(),
                    ManuscriptStatus.REJECTED,
                    manuscript.getStatus().allowedTransitions()
            );
        }

        ManuscriptStatus oldStatus = manuscript.getStatus();
        manuscript.setStatus(ManuscriptStatus.REJECTED);
        Manuscript updated = manuscriptRepository.save(manuscript);

        logger.info(
                "Updated manuscript {} status {} -> REJECTED by chief editor {}",
                updated.getManuscriptId(),
                oldStatus,
                chiefEditorId
        );

        publisher.publishEvent(new ManuscriptRejectedEvent(
                updated.getManuscriptId(),
                updated.getTitle(),
                chiefEditorId,
                updated.getAuthorId(),
                request.reason()
        ));
        return ManuscriptResponse.from(updated);
    }

    @Override
    @Transactional
    public ManuscriptResponse postponeManuscript(UUID id, UUID chiefEditorId, ManuscriptPostponementRequest request) {
        Manuscript manuscript = manuscriptRepository.findByIdWithGenres(id)
                .orElseThrow(() -> new ManuscriptNotFoundException(id));
        if (!manuscript.getStatus().canTransitionTo(ManuscriptStatus.POSTPONED)) {
            throw new InvalidStateTransitionException(
                    "Manuscript",
                    id,
                    manuscript.getStatus(),
                    ManuscriptStatus.POSTPONED,
                    manuscript.getStatus().allowedTransitions()
            );
        }

        ManuscriptStatus oldStatus = manuscript.getStatus();
        manuscript.setStatus(ManuscriptStatus.POSTPONED);
        Manuscript updated = manuscriptRepository.save(manuscript);

        logger.info(
                "Updated manuscript {} status {} -> POSTPONED by chief editor {}",
                updated.getManuscriptId(),
                oldStatus,
                chiefEditorId
        );

        publisher.publishEvent(new ManuscriptPostponedEvent(
                updated.getManuscriptId(),
                updated.getTitle(),
                chiefEditorId,
                updated.getAuthorId(),
                request.comment()
        ));
        return ManuscriptResponse.from(updated);
    }

    @Override
    @Transactional
    public ManuscriptResponse submitManuscript(UUID authorId, ManuscriptSubmissionRequest request) {
        Set<Genre> genres = new HashSet<>();
        if(request.genreIds() != null && !request.genreIds().isEmpty()) {
            genres.addAll(genreRepository.findAllById(request.genreIds()));
        }

        Manuscript manuscript = new Manuscript(
                null,
                request.title(),
                authorId,
                ManuscriptStatus.SUBMITTED,
                genres,
                request.annotation(),
                request.draftFileUrl(),
                Instant.now()
        );
        Manuscript saved = manuscriptRepository.save(manuscript);

        logger.info(
                "Created manuscript {} '{}' by author {}",
                saved.getManuscriptId(),
                saved.getTitle(),
                saved.getAuthorId()
        );

        publisher.publishEvent(new ManuscriptSubmittedEvent(
                saved.getManuscriptId(),
                saved.getTitle(),
                saved.getAuthorId()
        ));
        return ManuscriptResponse.from(saved);
    }
}
