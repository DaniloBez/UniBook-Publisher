package com.unibook.publisher.production.service;

import com.unibook.publisher.common.enums.UserRole;
import com.unibook.publisher.common.exception.notfound.ManuscriptNotFoundException;
import com.unibook.publisher.common.logging.AppLogger;
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
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TeamAssignmentServiceImplTest {

    @Mock
    private TeamAssignmentRepository teamAssignmentRepository;

    @Mock
    private ManuscriptRepository manuscriptRepository;

    @Mock
    private AppLogger logger;

    @InjectMocks
    private TeamAssignmentServiceImpl teamAssignmentService;

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
                Set.of(),
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
        UUID randomUserId = UUID.randomUUID();
        when(manuscriptRepository.findById(manuscriptId)).thenReturn(Optional.empty());
        assertThrows(ManuscriptNotFoundException.class, () -> teamAssignmentService.assign(manuscriptId, randomUserId, UserRole.EDITOR));
        verify(teamAssignmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Повернення id рукописів користувача за роллю")
    void getManuscriptIdsForUser_Success() {
        UUID userId = UUID.randomUUID();
        List<UUID> ids = List.of(UUID.randomUUID(), UUID.randomUUID());
        when(teamAssignmentRepository.findManuscriptIdsByUserIdAndRole(userId, UserRole.DESIGNER)).thenReturn(ids);

        List<UUID> result = teamAssignmentService.getManuscriptIdsForUser(userId, UserRole.DESIGNER);

        assertEquals(ids, result);
        verify(teamAssignmentRepository, times(1)).findManuscriptIdsByUserIdAndRole(userId, UserRole.DESIGNER);
    }

    @Test
    @DisplayName("Порожній список, якщо призначень немає")
    void getManuscriptIdsForUser_Empty() {
        UUID userId = UUID.randomUUID();
        when(teamAssignmentRepository.findManuscriptIdsByUserIdAndRole(userId, UserRole.EDITOR)).thenReturn(List.of());

        List<UUID> result = teamAssignmentService.getManuscriptIdsForUser(userId, UserRole.EDITOR);

        assertTrue(result.isEmpty());
        verify(teamAssignmentRepository, times(1)).findManuscriptIdsByUserIdAndRole(userId, UserRole.EDITOR);
    }
}
