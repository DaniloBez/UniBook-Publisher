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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CoverVersionServiceImplTest {

    @Mock
    private CoverVersionRepository coverVersionRepository;

    @Mock
    private ManuscriptRepository manuscriptRepository;

    @Mock
    private TeamAssignmentRepository teamAssignmentRepository;

    @Mock
    private ApplicationEventPublisher publisher;

    @Mock
    private AppLogger logger;

    @InjectMocks
    private CoverVersionServiceImpl coverVersionService;

    private Manuscript manuscript(UUID manuscriptId, ManuscriptStatus status) {
        return new Manuscript(manuscriptId, "Дюна", UUID.randomUUID(), status, List.of(), "Анотація", "url", Instant.now());
    }

    @Test
    @DisplayName("Успішне завантаження обкладинки призначеним дизайнером")
    void uploadCoverVersion_Success() {
        UUID manuscriptId = UUID.randomUUID();
        UUID designerId = UUID.randomUUID();
        Manuscript manuscript = manuscript(manuscriptId, ManuscriptStatus.IN_DESIGN);
        TeamAssignment designerAssignment = new TeamAssignment(UUID.randomUUID(), manuscriptId, designerId, UserRole.DESIGNER, Instant.now());

        when(manuscriptRepository.findById(manuscriptId)).thenReturn(Optional.of(manuscript));
        when(teamAssignmentRepository.findByManuscriptIdAndRole(manuscriptId, UserRole.DESIGNER)).thenReturn(Optional.of(designerAssignment));
        when(coverVersionRepository.findLatestByManuscriptId(manuscriptId)).thenReturn(Optional.empty());
        when(coverVersionRepository.save(any(CoverVersion.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CoverVersionResponse response = coverVersionService.uploadCoverVersion(manuscriptId, designerId, new CoverVersionRequest("cover.png"));

        assertEquals(1, response.versionNumber());
        assertEquals(designerId, response.uploadedByUserId());
        verify(publisher, times(1)).publishEvent(any(CoverVersionAddedEvent.class));
    }

    @Test
    @DisplayName("Заборонено завантажувати обкладинку не призначеному дизайнеру")
    void uploadCoverVersion_ForbiddenWhenNotAssignedDesigner() {
        UUID manuscriptId = UUID.randomUUID();
        Manuscript manuscript = manuscript(manuscriptId, ManuscriptStatus.IN_DESIGN);
        TeamAssignment designerAssignment = new TeamAssignment(UUID.randomUUID(), manuscriptId, UUID.randomUUID(), UserRole.DESIGNER, Instant.now());

        when(manuscriptRepository.findById(manuscriptId)).thenReturn(Optional.of(manuscript));
        when(teamAssignmentRepository.findByManuscriptIdAndRole(manuscriptId, UserRole.DESIGNER)).thenReturn(Optional.of(designerAssignment));

        assertThrows(ForbiddenActionException.class,
                () -> coverVersionService.uploadCoverVersion(manuscriptId, UUID.randomUUID(), new CoverVersionRequest("cover.png")));
        verify(coverVersionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Заборонено завантажувати обкладинку поза статусом IN_DESIGN")
    void uploadCoverVersion_InvalidStateWhenNotInDesign() {
        UUID manuscriptId = UUID.randomUUID();
        UUID designerId = UUID.randomUUID();
        Manuscript manuscript = manuscript(manuscriptId, ManuscriptStatus.TEXT_APPROVED);
        TeamAssignment designerAssignment = new TeamAssignment(UUID.randomUUID(), manuscriptId, designerId, UserRole.DESIGNER, Instant.now());

        when(manuscriptRepository.findById(manuscriptId)).thenReturn(Optional.of(manuscript));
        when(teamAssignmentRepository.findByManuscriptIdAndRole(manuscriptId, UserRole.DESIGNER)).thenReturn(Optional.of(designerAssignment));

        assertThrows(InvalidStateTransitionException.class,
                () -> coverVersionService.uploadCoverVersion(manuscriptId, designerId, new CoverVersionRequest("cover.png")));
        verify(coverVersionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Помилка, якщо рукопис не знайдено при завантаженні обкладинки")
    void uploadCoverVersion_ManuscriptNotFoundException() {
        UUID manuscriptId = UUID.randomUUID();
        when(manuscriptRepository.findById(manuscriptId)).thenReturn(Optional.empty());

        assertThrows(ManuscriptNotFoundException.class,
                () -> coverVersionService.uploadCoverVersion(manuscriptId, UUID.randomUUID(), new CoverVersionRequest("cover.png")));
        verify(coverVersionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Заборонено завантажувати обкладинку, якщо дизайнера взагалі не призначено")
    void uploadCoverVersion_ForbiddenWhenNoDesignerAssigned() {
        UUID manuscriptId = UUID.randomUUID();
        Manuscript manuscript = manuscript(manuscriptId, ManuscriptStatus.IN_DESIGN);

        when(manuscriptRepository.findById(manuscriptId)).thenReturn(Optional.of(manuscript));
        when(teamAssignmentRepository.findByManuscriptIdAndRole(manuscriptId, UserRole.DESIGNER)).thenReturn(Optional.empty());

        assertThrows(ForbiddenActionException.class,
                () -> coverVersionService.uploadCoverVersion(manuscriptId, UUID.randomUUID(), new CoverVersionRequest("cover.png")));
        verify(coverVersionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Успішне отримання списку версій обкладинок рукопису")
    void getCoverVersions_Success() {
        UUID manuscriptId = UUID.randomUUID();
        CoverVersion coverVersion = new CoverVersion(UUID.randomUUID(), manuscriptId, "cover.png", UUID.randomUUID(), 1, Instant.now());

        when(manuscriptRepository.findById(manuscriptId)).thenReturn(Optional.of(manuscript(manuscriptId, ManuscriptStatus.IN_DESIGN)));
        when(coverVersionRepository.findByManuscriptId(manuscriptId)).thenReturn(List.of(coverVersion));

        List<CoverVersionResponse> responses = coverVersionService.getCoverVersions(manuscriptId);

        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals(1, responses.get(0).versionNumber());
    }

    @Test
    @DisplayName("Помилка, якщо рукопис не знайдено при отриманні версій обкладинок")
    void getCoverVersions_ManuscriptNotFoundException() {
        UUID manuscriptId = UUID.randomUUID();
        when(manuscriptRepository.findById(manuscriptId)).thenReturn(Optional.empty());

        assertThrows(ManuscriptNotFoundException.class, () -> coverVersionService.getCoverVersions(manuscriptId));
    }
}
