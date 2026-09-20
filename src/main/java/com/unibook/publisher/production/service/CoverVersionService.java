package com.unibook.publisher.production.service;

import com.unibook.publisher.common.enums.UserRole;
import com.unibook.publisher.common.event.CoverVersionAddedEvent;
import com.unibook.publisher.common.exception.notfound.ManuscriptNotFoundException;
import com.unibook.publisher.common.exception.security.ForbiddenActionException;
import com.unibook.publisher.common.exception.state.InvalidStateTransitionException;
import com.unibook.publisher.production.entity.CoverVersion;
import com.unibook.publisher.production.entity.Manuscript;
import com.unibook.publisher.production.enums.ManuscriptStatus;
import com.unibook.publisher.production.entity.TeamAssignment;
import com.unibook.publisher.production.entity.request.CoverVersionRequest;
import com.unibook.publisher.production.entity.response.CoverVersionResponse;
import com.unibook.publisher.production.repository.CoverVersionRepository;
import com.unibook.publisher.production.repository.ManuscriptRepository;
import com.unibook.publisher.production.repository.TeamAssignmentRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class CoverVersionService {
    private final CoverVersionRepository coverVersionRepository;
    private final ManuscriptRepository manuscriptRepository;
    private final TeamAssignmentRepository teamAssignmentRepository;
    private final ApplicationEventPublisher publisher;

    public CoverVersionService(
            CoverVersionRepository coverVersionRepository,
            ManuscriptRepository manuscriptRepository,
            TeamAssignmentRepository teamAssignmentRepository,
            ApplicationEventPublisher publisher
    ) {
        this.coverVersionRepository = coverVersionRepository;
        this.manuscriptRepository = manuscriptRepository;
        this.teamAssignmentRepository = teamAssignmentRepository;
        this.publisher = publisher;
    }

    public CoverVersionResponse uploadCoverVersion(UUID manuscriptId, UUID designerId, CoverVersionRequest request) {
        Manuscript manuscript = manuscriptRepository.findById(manuscriptId)
                .orElseThrow(() -> new ManuscriptNotFoundException(manuscriptId));

        TeamAssignment designerAssignment = teamAssignmentRepository.findByManuscriptIdAndRole(manuscriptId, UserRole.DESIGNER)
                .orElseThrow(() -> new ForbiddenActionException("Дизайнера не призначено на цей рукопис"));
        if (!designerAssignment.userId().equals(designerId)) {
            throw new ForbiddenActionException("Завантажувати обкладинку може лише призначений дизайнер");
        }
        if (manuscript.status() != ManuscriptStatus.IN_DESIGN) {
            throw new InvalidStateTransitionException(
                    "Manuscript",
                    manuscriptId,
                    manuscript.status(),
                    ManuscriptStatus.IN_DESIGN,
                    manuscript.status().allowedTransitions()
            );
        }

        int versionNumber = coverVersionRepository.findLatestByManuscriptId(manuscriptId)
                .map(cv -> cv.versionNumber() + 1)
                .orElse(1);

        CoverVersion saved = coverVersionRepository.save(new CoverVersion(
                UUID.randomUUID(),
                manuscriptId,
                request.fileUrl(),
                designerId,
                versionNumber,
                Instant.now()
        ));

        publisher.publishEvent(new CoverVersionAddedEvent(
                manuscript.manuscriptId(),
                manuscript.title(),
                saved.id(),
                designerId,
                manuscript.authorId()
        ));

        return CoverVersionResponse.from(saved);
    }

    public List<CoverVersionResponse> getCoverVersions(UUID manuscriptId) {
        if (manuscriptRepository.findById(manuscriptId).isEmpty()) {
            throw new ManuscriptNotFoundException(manuscriptId);
        }
        return coverVersionRepository.findByManuscriptId(manuscriptId).stream()
                .map(CoverVersionResponse::from)
                .toList();
    }
}
