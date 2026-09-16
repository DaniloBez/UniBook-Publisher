package com.unibook.publisher.production.service;

import com.unibook.publisher.common.enums.UserRole;
import com.unibook.publisher.common.exception.ResourceNotFoundException;
import com.unibook.publisher.production.entity.TeamAssignment;
import com.unibook.publisher.production.entity.response.TeamAssignmentResponse;
import com.unibook.publisher.production.repository.ManuscriptRepository;
import com.unibook.publisher.production.repository.TeamAssignmentRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class TeamAssignmentService {
    private final TeamAssignmentRepository teamAssignmentRepository;
    private final ManuscriptRepository manuscriptRepository;

    public TeamAssignmentService(TeamAssignmentRepository teamAssignmentRepository, ManuscriptRepository manuscriptRepository) {
        this.teamAssignmentRepository = teamAssignmentRepository;
        this.manuscriptRepository = manuscriptRepository;
    }

    public TeamAssignmentResponse assign(UUID manuscriptId, UUID userId, UserRole role) {
        if(manuscriptRepository.findById(manuscriptId).isEmpty()) {
            throw new ResourceNotFoundException("Рукопис з ID " + manuscriptId + " не знайдено");
        }
        TeamAssignment assignment = new TeamAssignment(
                UUID.randomUUID(),
                manuscriptId,
                userId,
                role,
                Instant.now()
        );
        return TeamAssignmentResponse.from(teamAssignmentRepository.save(assignment));
    }
}
