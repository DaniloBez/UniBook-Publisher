package com.unibook.publisher.production.service;

import com.unibook.publisher.common.enums.UserRole;
import com.unibook.publisher.common.exception.notfound.ManuscriptNotFoundException;
import com.unibook.publisher.common.logging.AppLogger;
import com.unibook.publisher.production.entity.TeamAssignment;
import com.unibook.publisher.production.entity.response.TeamAssignmentResponse;
import com.unibook.publisher.production.repository.ManuscriptRepository;
import com.unibook.publisher.production.repository.TeamAssignmentRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
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
    public TeamAssignmentResponse assign(UUID manuscriptId, UUID userId, UserRole role) {
        if(manuscriptRepository.findById(manuscriptId).isEmpty())
            throw new ManuscriptNotFoundException(manuscriptId);

        TeamAssignment assignment = new TeamAssignment(
                UUID.randomUUID(),
                manuscriptId,
                userId,
                role,
                Instant.now()
        );

        logger.info(
            "Created team assignment {}: user {} assigned as {} to manuscript {}",
            assignment.teamId(),
            userId,
            role,
            manuscriptId
        );

        return TeamAssignmentResponse.from(teamAssignmentRepository.save(assignment));
    }
}
