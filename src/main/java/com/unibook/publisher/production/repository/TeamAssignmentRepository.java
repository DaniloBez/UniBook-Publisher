package com.unibook.publisher.production.repository;

import com.unibook.publisher.production.entity.TeamAssignment;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class TeamAssignmentRepository {
    private final Map<UUID, TeamAssignment> assignments = new ConcurrentHashMap<>();

    public TeamAssignment save (TeamAssignment assignment) {
        assignments.put(assignment.teamId(), assignment);
        return assignment;
    }
}
