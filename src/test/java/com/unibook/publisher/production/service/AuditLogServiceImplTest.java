package com.unibook.publisher.production.service;

import com.unibook.publisher.production.entity.ManuscriptAuditLog;
import com.unibook.publisher.production.enums.ManuscriptStatus;
import com.unibook.publisher.production.entity.response.AuditLogResponse;
import com.unibook.publisher.production.repository.ManuscriptAuditLogRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuditLogServiceImplTest {

    @Mock
    private ManuscriptAuditLogRepository auditLogRepository;

    @InjectMocks
    private AuditLogServiceImpl auditLogService;

    @Test
    @DisplayName("Запис аудиту зберігається зі старим і новим статусом")
    void record_SavesLogWithGivenStatuses() {
        UUID manuscriptId = UUID.randomUUID();
        UUID changedByUserId = UUID.randomUUID();

        auditLogService.record(manuscriptId, changedByUserId, ManuscriptStatus.IN_PROGRESS, ManuscriptStatus.TEXT_APPROVED);

        ArgumentCaptor<ManuscriptAuditLog> captor = ArgumentCaptor.forClass(ManuscriptAuditLog.class);
        verify(auditLogRepository, times(1)).save(captor.capture());

        ManuscriptAuditLog saved = captor.getValue();
        assertEquals(manuscriptId, saved.manuscriptId());
        assertEquals(changedByUserId, saved.changedByUserId());
        assertEquals(ManuscriptStatus.IN_PROGRESS, saved.oldStatus());
        assertEquals(ManuscriptStatus.TEXT_APPROVED, saved.newStatus());
    }

    @Test
    @DisplayName("Історія аудиту повертається у вигляді відповідей")
    void getAuditLog_ReturnsMappedResponses() {
        UUID manuscriptId = UUID.randomUUID();
        ManuscriptAuditLog log = new ManuscriptAuditLog(
                UUID.randomUUID(), manuscriptId, UUID.randomUUID(),
                ManuscriptStatus.SUBMITTED, ManuscriptStatus.IN_PROGRESS, Instant.now()
        );
        when(auditLogRepository.findByManuscriptId(manuscriptId)).thenReturn(List.of(log));

        List<AuditLogResponse> result = auditLogService.getAuditLog(manuscriptId);

        assertEquals(1, result.size());
        assertEquals(ManuscriptStatus.SUBMITTED, result.get(0).oldStatus());
        assertEquals(ManuscriptStatus.IN_PROGRESS, result.get(0).newStatus());
    }
}
