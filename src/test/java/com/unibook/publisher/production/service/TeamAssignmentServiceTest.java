package com.unibook.publisher.production.service;

import com.unibook.publisher.common.enums.UserRole;
import com.unibook.publisher.common.exception.notfound.ManuscriptNotFoundException;
import com.unibook.publisher.production.entity.Manuscript;
import com.unibook.publisher.production.enums.ManuscriptStatus;
import com.unibook.publisher.production.entity.TeamAssignment;
import com.unibook.publisher.production.entity.response.TeamAssignmentResponse;
import com.unibook.publisher.production.repository.ManuscriptRepository;
import com.unibook.publisher.production.repository.TeamAssignmentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TeamAssignmentServiceTest {

    @Mock
    private TeamAssignmentRepository teamAssignmentRepository;

    @Mock
    private ManuscriptRepository manuscriptRepository;

    @InjectMocks
    private TeamAssignmentService teamAssignmentService;

    @Test
    @DisplayName("Успішне призначення користувача в команду")
    void assign_Success() {
        UUID manuscriptId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Manuscript manuscript = new Manuscript(
                manuscriptId,
                "Гаррі Поттер",
                UUID.randomUUID(),
                ManuscriptStatus.IN_PROGRESS,
                List.of(),
                "Анотація до книги Гаррі Поттер",
                "url",
                Instant.now()
        );
        when(manuscriptRepository.findById(manuscriptId)).thenReturn(Optional.of(manuscript));
        when(teamAssignmentRepository.save(any(TeamAssignment.class))).thenAnswer(i -> i.getArgument(0));

        TeamAssignmentResponse response = teamAssignmentService.assign(manuscriptId, userId, UserRole.EDITOR);
        assertNotNull(response);
        assertEquals(manuscriptId, response.manuscriptId());
        assertEquals(userId, response.userId());
        assertEquals(UserRole.EDITOR, response.role());

        verify(teamAssignmentRepository, times(1)).save(any(TeamAssignment.class));
    }

    @Test
    @DisplayName("Рукопис не знайдено")
    void assign_ResourceNotFoundException() {
        UUID manuscriptId = UUID.randomUUID();
        when(manuscriptRepository.findById(manuscriptId)).thenReturn(Optional.empty());
        assertThrows(ManuscriptNotFoundException.class, () -> teamAssignmentService.assign(manuscriptId, UUID.randomUUID(), UserRole.EDITOR));
        verify(teamAssignmentRepository, never()).save(any());
    }
}
