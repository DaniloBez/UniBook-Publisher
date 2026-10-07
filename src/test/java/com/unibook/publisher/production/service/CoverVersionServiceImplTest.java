package com.unibook.publisher.production.service;

import com.unibook.publisher.common.enums.UserRole;
import com.unibook.publisher.common.event.CoverVersionAddedEvent;
import com.unibook.publisher.common.exception.badrequest.InvalidFileTypeException;
import com.unibook.publisher.common.exception.notfound.ManuscriptNotFoundException;
import com.unibook.publisher.common.exception.security.ForbiddenActionException;
import com.unibook.publisher.common.exception.state.InvalidStateTransitionException;
import com.unibook.publisher.common.exception.business.FileUploadException;
import com.unibook.publisher.common.logging.AppLogger;
import com.unibook.publisher.production.entity.CoverVersion;
import com.unibook.publisher.production.entity.Manuscript;
import com.unibook.publisher.production.entity.TeamAssignment;
import com.unibook.publisher.production.entity.response.CoverVersionResponse;
import com.unibook.publisher.production.enums.ManuscriptStatus;
import com.unibook.publisher.production.repository.CoverVersionRepository;
import com.unibook.publisher.production.repository.ManuscriptRepository;
import com.unibook.publisher.production.repository.TeamAssignmentRepository;
import com.unibook.publisher.storage.FileStorageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
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
    private FileStorageService fileStorageService;

    @Mock
    private ApplicationEventPublisher publisher;

    @Mock
    private AppLogger logger;

    @InjectMocks
    private CoverVersionServiceImpl coverVersionService;

    private Manuscript manuscript(UUID manuscriptId, ManuscriptStatus status) {
        return new Manuscript(manuscriptId, "Дюна", UUID.randomUUID(), status, Set.of(), "Анотація", "url", Instant.now());
    }

    @Test
    @DisplayName("Успішне завантаження першої версії обкладинки")
    void uploadCoverVersion_Success() {
        UUID manuscriptId = UUID.randomUUID();
        UUID designerId = UUID.randomUUID();
        Manuscript manuscript = manuscript(manuscriptId, ManuscriptStatus.IN_DESIGN);
        TeamAssignment designerAssignment = new TeamAssignment(UUID.randomUUID(), manuscript, designerId, UserRole.DESIGNER, Instant.now());
        InputStream fileStream = new ByteArrayInputStream("fake-image-bytes".getBytes(StandardCharsets.UTF_8));
        String expectedPath = manuscript.getAuthorId() + "/" + manuscriptId + "/covers/cover_v1.png";

        when(manuscriptRepository.findById(manuscriptId)).thenReturn(Optional.of(manuscript));
        when(teamAssignmentRepository.findByManuscript_ManuscriptIdAndRole(manuscriptId, UserRole.DESIGNER)).thenReturn(Optional.of(designerAssignment));
        when(coverVersionRepository.findTopByManuscript_ManuscriptIdOrderByVersionNumberDesc(manuscriptId)).thenReturn(Optional.empty());
        when(fileStorageService.put(anyString(), any(InputStream.class), anyLong(), anyString())).thenReturn(expectedPath);
        when(coverVersionRepository.save(any(CoverVersion.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CoverVersionResponse response = coverVersionService.uploadCoverVersion(manuscriptId, designerId, fileStream, 1024L, "image/png", "cover.png");

        assertEquals(1, response.versionNumber());
        assertEquals(designerId, response.uploadedByUserId());
        assertEquals(expectedPath, response.fileUrl());
        verify(publisher, times(1)).publishEvent(any(CoverVersionAddedEvent.class));
    }

    @Test
    @DisplayName("Збільшення номера версії обкладинки, якщо попередні версії вже існують")
    void uploadCoverVersion_IncrementsVersionNumber() {
        UUID manuscriptId = UUID.randomUUID();
        UUID designerId = UUID.randomUUID();
        Manuscript manuscript = manuscript(manuscriptId, ManuscriptStatus.IN_DESIGN);
        TeamAssignment designerAssignment = new TeamAssignment(UUID.randomUUID(), manuscript, designerId, UserRole.DESIGNER, Instant.now());
        CoverVersion existingCover = new CoverVersion(manuscript, "old.png", designerId, 3, Instant.now());
        InputStream fileStream = new ByteArrayInputStream("fake-bytes".getBytes(StandardCharsets.UTF_8));
        String expectedPath = manuscript.getAuthorId() + "/" + manuscriptId + "/covers/cover_v4.png";

        when(manuscriptRepository.findById(manuscriptId)).thenReturn(Optional.of(manuscript));
        when(teamAssignmentRepository.findByManuscript_ManuscriptIdAndRole(manuscriptId, UserRole.DESIGNER)).thenReturn(Optional.of(designerAssignment));
        when(coverVersionRepository.findTopByManuscript_ManuscriptIdOrderByVersionNumberDesc(manuscriptId)).thenReturn(Optional.of(existingCover));
        when(fileStorageService.put(anyString(), any(InputStream.class), anyLong(), anyString())).thenReturn(expectedPath);
        when(coverVersionRepository.save(any(CoverVersion.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CoverVersionResponse response = coverVersionService.uploadCoverVersion(manuscriptId, designerId, fileStream, 1024L, "image/png", "cover.png");

        assertEquals(4, response.versionNumber());
    }

    @Test
    @DisplayName("Помилка валідації: некоректний Content-Type або null")
    void uploadCoverVersion_InvalidContentType_ThrowsException() {
        UUID manuscriptId = UUID.randomUUID();
        UUID designerId = UUID.randomUUID();
        InputStream fileStream = new ByteArrayInputStream("fake".getBytes(StandardCharsets.UTF_8));

        assertThrows(InvalidFileTypeException.class,
                () -> coverVersionService.uploadCoverVersion(manuscriptId, designerId, fileStream, 100L, "application/pdf", "cover.png"));

        assertThrows(InvalidFileTypeException.class,
                () -> coverVersionService.uploadCoverVersion(manuscriptId, designerId, fileStream, 100L, null, "cover.png"));
    }

    @Test
    @DisplayName("Помилка валідації: некоректна назва файлу (null, без крапки або крапка в кінці)")
    void uploadCoverVersion_InvalidFilename_ThrowsException() {
        UUID manuscriptId = UUID.randomUUID();
        UUID designerId = UUID.randomUUID();
        InputStream fileStream = new ByteArrayInputStream("fake".getBytes(StandardCharsets.UTF_8));

        assertThrows(InvalidFileTypeException.class,
                () -> coverVersionService.uploadCoverVersion(manuscriptId, designerId, fileStream, 100L, "image/png", null));

        assertThrows(InvalidFileTypeException.class,
                () -> coverVersionService.uploadCoverVersion(manuscriptId, designerId, fileStream, 100L, "image/png", "coverfilename"));

        assertThrows(InvalidFileTypeException.class,
                () -> coverVersionService.uploadCoverVersion(manuscriptId, designerId, fileStream, 100L, "image/png", "cover."));
    }

    @Test
    @DisplayName("Помилка валідації: непідтримуване розширення файлу")
    void uploadCoverVersion_InvalidExtension_ThrowsException() {
        UUID manuscriptId = UUID.randomUUID();
        UUID designerId = UUID.randomUUID();
        InputStream fileStream = new ByteArrayInputStream("fake".getBytes(StandardCharsets.UTF_8));

        assertThrows(InvalidFileTypeException.class,
                () -> coverVersionService.uploadCoverVersion(manuscriptId, designerId, fileStream, 100L, "image/png", "cover.gif"));
    }

    @Test
    @DisplayName("Відкат завантаження у сховищі при помилці збереження в базі даних")
    void uploadCoverVersion_RollbackOnDatabaseError() {
        UUID manuscriptId = UUID.randomUUID();
        UUID designerId = UUID.randomUUID();
        Manuscript manuscript = manuscript(manuscriptId, ManuscriptStatus.IN_DESIGN);
        TeamAssignment designerAssignment = new TeamAssignment(UUID.randomUUID(), manuscript, designerId, UserRole.DESIGNER, Instant.now());
        InputStream fileStream = new ByteArrayInputStream("fake-bytes".getBytes(StandardCharsets.UTF_8));
        String uploadedPath = manuscript.getAuthorId() + "/" + manuscriptId + "/covers/cover.png";

        when(manuscriptRepository.findById(manuscriptId)).thenReturn(Optional.of(manuscript));
        when(teamAssignmentRepository.findByManuscript_ManuscriptIdAndRole(manuscriptId, UserRole.DESIGNER)).thenReturn(Optional.of(designerAssignment));
        when(fileStorageService.put(anyString(), any(InputStream.class), anyLong(), anyString())).thenReturn(uploadedPath);
        when(coverVersionRepository.findTopByManuscript_ManuscriptIdOrderByVersionNumberDesc(manuscriptId)).thenReturn(Optional.empty());
        when(coverVersionRepository.save(any())).thenThrow(new RuntimeException("Database error"));

        assertThrows(FileUploadException.class,
                () -> coverVersionService.uploadCoverVersion(manuscriptId, designerId, fileStream, 1024L, "image/png", "cover.png"));

        verify(fileStorageService, times(1)).delete(uploadedPath);
    }

    @Test
    @DisplayName("Обробка помилки при невдалій спробі відкату сховища")
    void uploadCoverVersion_RollbackCleanupFails_StillThrowsUploadException() {
        UUID manuscriptId = UUID.randomUUID();
        UUID designerId = UUID.randomUUID();
        Manuscript manuscript = manuscript(manuscriptId, ManuscriptStatus.IN_DESIGN);
        TeamAssignment designerAssignment = new TeamAssignment(UUID.randomUUID(), manuscript, designerId, UserRole.DESIGNER, Instant.now());
        InputStream fileStream = new ByteArrayInputStream("fake-bytes".getBytes(StandardCharsets.UTF_8));
        String uploadedPath = manuscript.getAuthorId() + "/" + manuscriptId + "/covers/cover.png";

        when(manuscriptRepository.findById(manuscriptId)).thenReturn(Optional.of(manuscript));
        when(teamAssignmentRepository.findByManuscript_ManuscriptIdAndRole(manuscriptId, UserRole.DESIGNER)).thenReturn(Optional.of(designerAssignment));
        when(fileStorageService.put(anyString(), any(InputStream.class), anyLong(), anyString())).thenReturn(uploadedPath);
        when(coverVersionRepository.findTopByManuscript_ManuscriptIdOrderByVersionNumberDesc(manuscriptId)).thenReturn(Optional.empty());
        when(coverVersionRepository.save(any())).thenThrow(new RuntimeException("Database error"));
        doThrow(new RuntimeException("MinIO delete failed")).when(fileStorageService).delete(uploadedPath);

        assertThrows(FileUploadException.class,
                () -> coverVersionService.uploadCoverVersion(manuscriptId, designerId, fileStream, 1024L, "image/png", "cover.png"));

        verify(fileStorageService, times(1)).delete(uploadedPath);
    }

    @Test
    @DisplayName("Заборонено завантажувати обкладинку не призначеному дизайнеру")
    void uploadCoverVersion_ForbiddenWhenNotAssignedDesigner() {
        UUID manuscriptId = UUID.randomUUID();
        Manuscript manuscript = manuscript(manuscriptId, ManuscriptStatus.IN_DESIGN);
        TeamAssignment designerAssignment = new TeamAssignment(UUID.randomUUID(), manuscript, UUID.randomUUID(), UserRole.DESIGNER, Instant.now());
        InputStream fileStream = new ByteArrayInputStream("fake-image-bytes".getBytes(StandardCharsets.UTF_8));

        when(manuscriptRepository.findById(manuscriptId)).thenReturn(Optional.of(manuscript));
        when(teamAssignmentRepository.findByManuscript_ManuscriptIdAndRole(manuscriptId, UserRole.DESIGNER)).thenReturn(Optional.of(designerAssignment));

        assertThrows(ForbiddenActionException.class,
                () -> coverVersionService.uploadCoverVersion(manuscriptId, UUID.randomUUID(), fileStream, 1024L, "image/png", "cover.png"));
        verify(coverVersionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Заборонено завантажувати обкладинку поза статусом IN_DESIGN")
    void uploadCoverVersion_InvalidStateWhenNotInDesign() {
        UUID manuscriptId = UUID.randomUUID();
        UUID designerId = UUID.randomUUID();
        Manuscript manuscript = manuscript(manuscriptId, ManuscriptStatus.TEXT_APPROVED);
        TeamAssignment designerAssignment = new TeamAssignment(UUID.randomUUID(), manuscript, designerId, UserRole.DESIGNER, Instant.now());
        InputStream fileStream = new ByteArrayInputStream("fake-image-bytes".getBytes(StandardCharsets.UTF_8));

        when(manuscriptRepository.findById(manuscriptId)).thenReturn(Optional.of(manuscript));
        when(teamAssignmentRepository.findByManuscript_ManuscriptIdAndRole(manuscriptId, UserRole.DESIGNER)).thenReturn(Optional.of(designerAssignment));

        assertThrows(InvalidStateTransitionException.class,
                () -> coverVersionService.uploadCoverVersion(manuscriptId, designerId, fileStream, 1024L, "image/png", "cover.png"));
        verify(coverVersionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Помилка, якщо рукопис не знайдено при завантаженні обкладинки")
    void uploadCoverVersion_ManuscriptNotFoundException() {
        UUID manuscriptId = UUID.randomUUID();
        InputStream fileStream = new ByteArrayInputStream("fake-image-bytes".getBytes(StandardCharsets.UTF_8));

        when(manuscriptRepository.findById(manuscriptId)).thenReturn(Optional.empty());

        assertThrows(ManuscriptNotFoundException.class,
                () -> coverVersionService.uploadCoverVersion(manuscriptId, UUID.randomUUID(), fileStream, 1024L, "image/png", "cover.png"));
        verify(coverVersionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Заборонено завантажувати обкладинку, якщо дизайнера взагалі не призначено")
    void uploadCoverVersion_ForbiddenWhenNoDesignerAssigned() {
        UUID manuscriptId = UUID.randomUUID();
        Manuscript manuscript = manuscript(manuscriptId, ManuscriptStatus.IN_DESIGN);
        InputStream fileStream = new ByteArrayInputStream("fake-image-bytes".getBytes(StandardCharsets.UTF_8));

        when(manuscriptRepository.findById(manuscriptId)).thenReturn(Optional.of(manuscript));
        when(teamAssignmentRepository.findByManuscript_ManuscriptIdAndRole(manuscriptId, UserRole.DESIGNER)).thenReturn(Optional.empty());

        assertThrows(ForbiddenActionException.class,
                () -> coverVersionService.uploadCoverVersion(manuscriptId, UUID.randomUUID(), fileStream, 1024L, "image/png", "cover.png"));
        verify(coverVersionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Успішне отримання списку версій обкладинок рукопису")
    void getCoverVersions_Success() {
        UUID manuscriptId = UUID.randomUUID();
        Manuscript manuscript = manuscript(manuscriptId, ManuscriptStatus.IN_DESIGN);
        CoverVersion coverVersion = new CoverVersion(manuscript, "cover.png", UUID.randomUUID(), 1, Instant.now());

        when(manuscriptRepository.findById(manuscriptId)).thenReturn(Optional.of(manuscript));
        when(coverVersionRepository.findByManuscript_ManuscriptIdOrderByVersionNumberAsc(manuscriptId)).thenReturn(List.of(coverVersion));

        List<CoverVersionResponse> responses = coverVersionService.getCoverVersions(manuscriptId);

        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals(1, responses.getFirst().versionNumber());
    }

    @Test
    @DisplayName("Помилка, якщо рукопис не знайдено при отриманні версій обкладинок")
    void getCoverVersions_ManuscriptNotFoundException() {
        UUID manuscriptId = UUID.randomUUID();
        when(manuscriptRepository.findById(manuscriptId)).thenReturn(Optional.empty());

        assertThrows(ManuscriptNotFoundException.class, () -> coverVersionService.getCoverVersions(manuscriptId));
    }
}