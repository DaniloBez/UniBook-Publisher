package com.unibook.publisher.production.service;

import com.unibook.publisher.common.logging.AppLogger;
import com.unibook.publisher.production.entity.Manuscript;
import com.unibook.publisher.production.entity.ManuscriptAuditLog;
import com.unibook.publisher.production.enums.ManuscriptStatus;
import com.unibook.publisher.production.entity.response.ManuscriptAuditLogResponse;
import com.unibook.publisher.production.repository.ManuscriptAuditLogRepository;
import com.unibook.publisher.production.repository.ManuscriptRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ManuscriptAuditLogServiceImplTest {

    @Mock
    private ManuscriptAuditLogRepository auditLogRepository;

    @Mock
    private ManuscriptRepository manuscriptRepository;

    @Mock
    private AppLogger logger;

    @InjectMocks
    private ManuscriptAuditLogServiceImpl auditLogService;

    @Test
    @DisplayName("Запис аудиту зберігається зі старим і новим статусом")
    void record_StatusChange_SavesLogWithGivenStatuses() {
        UUID manuscriptId = UUID.randomUUID();
        UUID changedByUserId = UUID.randomUUID();

        Manuscript manuscript = new Manuscript();
        manuscript.setManuscriptId(manuscriptId);
        when(manuscriptRepository.findById(manuscriptId)).thenReturn(Optional.of(manuscript));

        auditLogService.recordStatusChange(
                manuscriptId,
                changedByUserId,
                ManuscriptStatus.IN_PROGRESS,
                ManuscriptStatus.TEXT_APPROVED);

        ArgumentCaptor<ManuscriptAuditLog> captor = ArgumentCaptor.forClass(ManuscriptAuditLog.class);
        verify(auditLogRepository, times(1)).save(captor.capture());

        ManuscriptAuditLog saved = captor.getValue();
        assertEquals(manuscriptId, saved.getManuscript().getManuscriptId());
        assertEquals(changedByUserId, saved.getChangedByUserId());
        assertEquals(ManuscriptStatus.IN_PROGRESS, saved.getOldStatus());
        assertEquals(ManuscriptStatus.TEXT_APPROVED, saved.getNewStatus());
    }

    @Test
    @DisplayName("Історія аудиту повертається у вигляді відповідей")
    void getAuditLog_ReturnsMappedResponses() {
        UUID manuscriptId = UUID.randomUUID();

        Manuscript manuscript = new Manuscript();
        manuscript.setManuscriptId(manuscriptId);
        when(manuscriptRepository.existsById(manuscriptId)).thenReturn(true);

        ManuscriptAuditLog log = new ManuscriptAuditLog(
                UUID.randomUUID(), manuscript, UUID.randomUUID(),
                ManuscriptStatus.SUBMITTED, ManuscriptStatus.IN_PROGRESS, Instant.now()
        );
        when(auditLogRepository.findByManuscriptId(manuscriptId)).thenReturn(List.of(log));

        List<ManuscriptAuditLogResponse> result = auditLogService.getAuditLog(manuscriptId);

        assertEquals(1, result.size());
        assertEquals(ManuscriptStatus.SUBMITTED, result.get(0).oldStatus());
        assertEquals(ManuscriptStatus.IN_PROGRESS, result.get(0).newStatus());
    }
}
