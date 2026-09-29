package com.unibook.publisher.production.service;

import com.unibook.publisher.common.enums.UserRole;
import com.unibook.publisher.common.exception.notfound.ManuscriptNotFoundException;
import com.unibook.publisher.common.logging.AppLogger;
import com.unibook.publisher.production.entity.Manuscript;
import com.unibook.publisher.production.entity.TeamAssignment;
import com.unibook.publisher.production.entity.response.TeamAssignmentResponse;
import com.unibook.publisher.production.repository.ManuscriptRepository;
import com.unibook.publisher.production.repository.TeamAssignmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class TeamAssignmentServiceImpl implements TeamAssignmentService {
    private final TeamAssignmentRepository teamAssignmentRepository;
    private final ManuscriptRepository manuscriptRepository;
    private final AppLogger logger;

    public TeamAssignmentServiceImpl(TeamAssignmentRepository teamAssignmentRepository, ManuscriptRepository manuscriptRepository, AppLogger logger) {
        this.teamAssignmentRepository = teamAssignmentRepository;
        this.manuscriptRepository = manuscriptRepository;
        this.logger = logger;
    }

    @Override
    @Transactional
    public TeamAssignmentResponse assign(UUID manuscriptId, UUID userId, UserRole role) {
        Manuscript manuscript = manuscriptRepository.findById(manuscriptId)
                .orElseThrow(() -> new ManuscriptNotFoundException(manuscriptId));

        TeamAssignment assignment = new TeamAssignment(
                null,
                manuscript,
                userId,
                role,
                Instant.now()
        );
        TeamAssignment saved = teamAssignmentRepository.save(assignment);

        logger.info(
            "Created team assignment {}: user {} assigned as {} to manuscript {}",
            saved.getAssignmentId(),
            userId,
            role,
            manuscriptId
        );

        return TeamAssignmentResponse.from(saved);
    }
}
