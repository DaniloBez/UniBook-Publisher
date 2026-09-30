package com.unibook.publisher.production.service;

import com.unibook.publisher.common.enums.UserRole;
import com.unibook.publisher.common.event.CoverVersionAddedEvent;
import com.unibook.publisher.common.exception.notfound.ManuscriptNotFoundException;
import com.unibook.publisher.common.exception.security.ForbiddenActionException;
import com.unibook.publisher.common.exception.state.InvalidStateTransitionException;
import com.unibook.publisher.common.logging.AppLogger;
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
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class CoverVersionServiceImpl implements CoverVersionService {
    private final CoverVersionRepository coverVersionRepository;
    private final ManuscriptRepository manuscriptRepository;
    private final TeamAssignmentRepository teamAssignmentRepository;
    private final ApplicationEventPublisher publisher;
    private final AppLogger logger;

    public CoverVersionServiceImpl(
            CoverVersionRepository coverVersionRepository,
            ManuscriptRepository manuscriptRepository,
            TeamAssignmentRepository teamAssignmentRepository,
            ApplicationEventPublisher publisher,
            AppLogger logger
    ) {
        this.coverVersionRepository = coverVersionRepository;
        this.manuscriptRepository = manuscriptRepository;
        this.teamAssignmentRepository = teamAssignmentRepository;
        this.publisher = publisher;
        this.logger = logger;
    }

    @Override
    @Transactional
    public CoverVersionResponse uploadCoverVersion(UUID manuscriptId, UUID designerId, CoverVersionRequest request) {
        Manuscript manuscript = manuscriptRepository.findById(manuscriptId)
                .orElseThrow(() -> new ManuscriptNotFoundException(manuscriptId));

        TeamAssignment designerAssignment = teamAssignmentRepository.findByManuscript_ManuscriptIdAndRole(manuscriptId, UserRole.DESIGNER)
                .orElseThrow(() -> new ForbiddenActionException("Дизайнера не призначено на цей рукопис"));
        if (!designerAssignment.getUserId().equals(designerId)) {
            throw new ForbiddenActionException("Завантажувати обкладинку може лише призначений дизайнер");
        }
        if (manuscript.getStatus() != ManuscriptStatus.IN_DESIGN) {
            throw new InvalidStateTransitionException(
                    "Manuscript",
                    manuscriptId,
                    manuscript.getStatus(),
                    ManuscriptStatus.IN_DESIGN,
                    manuscript.getStatus().allowedTransitions()
            );
        }

        int versionNumber = coverVersionRepository.findTopByManuscript_ManuscriptIdOrderByVersionNumberDesc(manuscriptId)
                .map(cv -> cv.getVersionNumber() + 1)
                .orElse(1);

        CoverVersion saved = coverVersionRepository.save(new CoverVersion(
                manuscript,
                request.fileUrl(),
                designerId,
                versionNumber,
                Instant.now()
        ));

        logger.info(
                "Created cover version {} version {} for manuscript {} by designer {}",
                saved.getId(),
                saved.getVersionNumber(),
                manuscriptId,
                designerId
        );

        publisher.publishEvent(new CoverVersionAddedEvent(
                manuscript.getManuscriptId(),
                manuscript.getTitle(),
                saved.getId(),
                designerId,
                manuscript.getAuthorId()
        ));

        return CoverVersionResponse.from(saved);
    }

    @Override
    public List<CoverVersionResponse> getCoverVersions(UUID manuscriptId) {
        if (manuscriptRepository.findById(manuscriptId).isEmpty()) {
            throw new ManuscriptNotFoundException(manuscriptId);
        }
        return coverVersionRepository.findByManuscript_ManuscriptIdOrderByVersionNumberAsc(manuscriptId).stream()
                .map(CoverVersionResponse::from)
                .toList();
    }
}
