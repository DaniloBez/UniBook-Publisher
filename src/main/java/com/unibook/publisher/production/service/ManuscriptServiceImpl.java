package com.unibook.publisher.production.service;

import com.unibook.publisher.common.enums.UserRole;
import com.unibook.publisher.common.event.*;
import com.unibook.publisher.common.exception.notfound.GenreNotFoundException;
import com.unibook.publisher.common.exception.notfound.ManuscriptNotFoundException;
import com.unibook.publisher.common.exception.state.InvalidStateTransitionException;
import com.unibook.publisher.common.logging.AppLogger;
import com.unibook.publisher.common.security.AuthenticatedUser;
import com.unibook.publisher.identity.api.UserLookupService;
import com.unibook.publisher.identity.api.UserSummary;
import com.unibook.publisher.production.entity.Genre;
import com.unibook.publisher.production.entity.Manuscript;
import com.unibook.publisher.production.entity.request.*;
import com.unibook.publisher.production.enums.ManuscriptStatus;
import com.unibook.publisher.production.entity.response.ManuscriptResponse;
import com.unibook.publisher.production.repository.GenreRepository;
import com.unibook.publisher.production.repository.ManuscriptRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Transactional(readOnly = true)
public class ManuscriptServiceImpl implements ManuscriptService {

    private final ManuscriptRepository manuscriptRepository;
    private final GenreRepository genreRepository;
    private final TeamAssignmentService teamAssignmentService;
    private final UserLookupService userLookupService;
    private final ApplicationEventPublisher publisher;
    private final AppLogger logger;

    public ManuscriptServiceImpl(ManuscriptRepository manuscriptRepository, GenreRepository genreRepository, TeamAssignmentService teamAssignmentService, UserLookupService userLookupService, ApplicationEventPublisher publisher, AppLogger logger) {
        this.manuscriptRepository = manuscriptRepository;
        this.genreRepository = genreRepository;
        this.teamAssignmentService = teamAssignmentService;
        this.userLookupService = userLookupService;
        this.publisher = publisher;
        this.logger = logger;
    }

    @Override
    public List<ManuscriptResponse> getMyManuscripts(AuthenticatedUser currentUser, ManuscriptStatus status) {
        UUID userId = currentUser.id();
        UserRole role = currentUser.getUserRole();

        List<Manuscript> manuscripts;
        manuscripts = switch (role) {
            case AUTHOR -> manuscriptRepository.findByAuthorIdWithGenres(userId);
            case EDITOR, DESIGNER -> manuscriptRepository.findByAssignedUserIdWithGenres(userId);
            default -> manuscriptRepository.findAllWithGenres();
        };

        if(status != null) {
            manuscripts = manuscripts.stream()
                    .filter(m -> m.getStatus() == status)
                    .toList();
        }

        Set<UUID> authorIds = manuscripts.stream()
                .map(Manuscript::getAuthorId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<UUID, UserSummary> userMap = userLookupService.findAllByIds(authorIds);
        return manuscripts.stream()
                .map(m -> ManuscriptResponse.from(m, userMap))
                .toList();
    }

    @Override
    @PreAuthorize("hasAnyRole('CHIEF_EDITOR', 'ACCOUNTANT', 'ADMIN')")
    public ManuscriptResponse getManuscriptById(UUID id) {
        Manuscript manuscript = manuscriptRepository.findByIdWithGenres(id)
                .orElseThrow(() -> new ManuscriptNotFoundException(id));
        return toManuscriptResponse(manuscript);
    }

    @Override
    @PreAuthorize("hasAnyRole('CHIEF_EDITOR', 'ACCOUNTANT', 'ADMIN')")
    public List<ManuscriptResponse> getManuscripts(ManuscriptStatus status) {
        List<Manuscript> manuscripts = (status == null) ? manuscriptRepository.findAllWithGenres() : manuscriptRepository.findByStatusWithGenres(status);
        Set<UUID> userIds = manuscripts.stream()
                .flatMap(m -> Stream.of(m.getAuthorId()))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<UUID, UserSummary> userMap = userLookupService.findAllByIds(userIds);

        return manuscripts.stream()
                .map(m -> ManuscriptResponse.from(m, userMap))
                .toList();
    }

    @Override
    @PreAuthorize("hasAnyRole('AUTHOR', 'EDITOR', 'DESIGNER')")
    public List<ManuscriptResponse> getManuscriptsForUser(UUID userId, UserRole role) {
        List<Manuscript> manuscripts;
        if (role == UserRole.AUTHOR) {
            manuscripts = manuscriptRepository.findByAuthorIdWithGenres(userId);
        } else {
            List<UUID> manuscriptIds = teamAssignmentService.getManuscriptIdsForUser(userId, role);
            if (manuscriptIds.isEmpty()) {
                return List.of();
            }
            manuscripts = manuscriptRepository.findAllByIdWithGenres(manuscriptIds);
        }

        Set<UUID> authorIds = manuscripts.stream()
                .map(Manuscript::getAuthorId)
                .collect(Collectors.toSet());
        Map<UUID, UserSummary> userMap = userLookupService.findAllByIds(authorIds);

        return manuscripts.stream()
                .map(m -> ManuscriptResponse.from(m, userMap))
                .toList();
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('CHIEF_EDITOR')")
    public ManuscriptResponse approveManuscript(UUID id, AuthenticatedUser currentUser, ManuscriptApprovalRequest request) {
        UUID chiefEditorId = currentUser.id();

        Manuscript manuscript = manuscriptRepository.findByIdWithGenres(id).orElseThrow(() -> new ManuscriptNotFoundException(id));
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
        return toManuscriptResponse(updated);
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('CHIEF_EDITOR')")
    public ManuscriptResponse rejectManuscript(UUID id, AuthenticatedUser currentUser, ManuscriptRejectionRequest request) {
        UUID chiefEditorId = currentUser.id();

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
        return toManuscriptResponse(updated);
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('CHIEF_EDITOR')")
    public ManuscriptResponse postponeManuscript(UUID id, AuthenticatedUser currentUser, ManuscriptPostponementRequest request) {
        UUID chiefEditorId = currentUser.id();

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
        return toManuscriptResponse(updated);
    }

    @Override
    @Transactional
    public ManuscriptResponse submitManuscript(AuthenticatedUser currentUser, ManuscriptSubmissionRequest request) {
        UUID authorId = currentUser.id();

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
        return toManuscriptResponse(saved);
    }

    @Override
    @Transactional
    public ManuscriptResponse updateManuscript(UUID id, ManuscriptUpdateRequest request) {
        Manuscript manuscript = manuscriptRepository.findByIdWithGenres(id)
                .orElseThrow(() -> new ManuscriptNotFoundException(id));

        Set<Genre> genres = new HashSet<>(genreRepository.findAllById(request.genreIds()));
        if (genres.size() != request.genreIds().size()) {
            Set<UUID> found = genres.stream()
                    .map(Genre::getGenreId).
                    collect(Collectors.toSet());
            Set<UUID> missing = request.genreIds().stream()
                    .filter(genreId -> !found.contains(genreId))
                    .collect(Collectors.toSet());
            throw new GenreNotFoundException("Не знайдено жанрів: " + missing);
        }

        manuscript.setTitle(request.title());
        manuscript.setAnnotation(request.annotation());
        manuscript.setDraftFileUrl(request.draftFileUrl());
        manuscript.getGenres().clear();
        manuscript.getGenres().addAll(genres);

        Manuscript updated = manuscriptRepository.save(manuscript);

        logger.info("Updated manuscript {}: title='{}', genres={}",
                id, updated.getTitle(), updated.getGenres().size());

        return toManuscriptResponse(updated);
    }

    @Override
    public void deleteManuscript(UUID id) {
        Manuscript manuscript = manuscriptRepository.findByIdWithGenres(id)
                .orElseThrow(() -> new ManuscriptNotFoundException(id));
        manuscript.getGenres().clear();
        manuscriptRepository.save(manuscript);
        manuscriptRepository.delete(manuscript);
        logger.info("Deleted manuscript {} '{}'", id, manuscript.getTitle());
    }

    private ManuscriptResponse toManuscriptResponse(Manuscript manuscript) {
        Map<UUID, UserSummary> userMap = userLookupService.findById(manuscript.getAuthorId())
                .map(summary -> Map.of(manuscript.getAuthorId(), summary))
                .orElse(Collections.emptyMap());

        return ManuscriptResponse.from(manuscript, userMap);
    }
}
