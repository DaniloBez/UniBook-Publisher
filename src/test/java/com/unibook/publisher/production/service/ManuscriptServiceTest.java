package com.unibook.publisher.production.service;

import com.unibook.publisher.common.enums.UserRole;
import com.unibook.publisher.common.event.*;
import com.unibook.publisher.common.exception.notfound.ManuscriptNotFoundException;
import com.unibook.publisher.common.exception.notfound.ResourceNotFoundException;
import com.unibook.publisher.common.exception.state.InvalidStateTransitionException;
import com.unibook.publisher.production.entity.Manuscript;
import com.unibook.publisher.production.enums.ManuscriptStatus;
import com.unibook.publisher.production.entity.request.ManuscriptApprovalRequest;
import com.unibook.publisher.production.entity.request.ManuscriptPostponementRequest;
import com.unibook.publisher.production.entity.request.ManuscriptRejectionRequest;
import com.unibook.publisher.production.entity.request.ManuscriptSubmissionRequest;
import com.unibook.publisher.production.entity.response.ManuscriptResponse;
import com.unibook.publisher.production.repository.ManuscriptRepository;
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
public class ManuscriptServiceTest {

    @Mock
    private ManuscriptRepository repository;

    @Mock
    private ApplicationEventPublisher publisher;

    @Mock
    private TeamAssignmentService teamAssignmentService;

    @InjectMocks
    private ManuscriptService manuscriptService;

    @Test
    void submitManuscript_Success() {
        ManuscriptSubmissionRequest request = new ManuscriptSubmissionRequest(
                "Гаррі Поттер",
                "Анотація до книги Гаррі Поттер",
                "url",
                List.of(UUID.randomUUID())
        );
        UUID manuscriptId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        Manuscript saved = new Manuscript(
                manuscriptId,
                request.title(),
                authorId,
                ManuscriptStatus.SUBMITTED,
                request.genreIds(),
                request.annotation(),
                request.draftFileUrl(),
                Instant.now()
        );
        when(repository.save(any(Manuscript.class))).thenReturn(saved);

        ManuscriptResponse response = manuscriptService.submitManuscript(authorId, request);
        assertNotNull(response);
        assertEquals(manuscriptId, response.manuscriptId());
        assertEquals("Гаррі Поттер", response.title());
        assertEquals(ManuscriptStatus.SUBMITTED, response.status());

        verify(repository, times(1)).save(any(Manuscript.class));
        verify(publisher, times(1)).publishEvent(any(ManuscriptSubmittedEvent.class));
    }

    @Test
    void approveManuscript_Submitted_Success() {
        UUID manuscriptId = UUID.randomUUID();
        UUID chiefEditorId = UUID.randomUUID();
        UUID editorId = UUID.randomUUID();

        Manuscript manuscript = new Manuscript(
                manuscriptId,
                "Аліса",
                UUID.randomUUID(),
                ManuscriptStatus.SUBMITTED,
                List.of(),
                "Опис до книги Аліса",
                "url",
                Instant.now()
        );
        when(repository.findById(manuscriptId)).thenReturn(Optional.of(manuscript));

        ManuscriptApprovalRequest request = new ManuscriptApprovalRequest(editorId);
        ManuscriptResponse response = manuscriptService.approveManuscript(manuscriptId, chiefEditorId, request);
        assertNotNull(response);
        assertEquals(ManuscriptStatus.IN_PROGRESS, response.status());

        verify(repository, times(1)).save(any(Manuscript.class));
        verify(publisher, times(1)).publishEvent(any(ManuscriptApprovedEvent.class));
        verify(teamAssignmentService, times(1)).assign(manuscriptId, editorId, UserRole.EDITOR);
        verify(publisher, times(1)).publishEvent(any(WorkerAssignedEvent.class));
    }

    @Test
    void approveManuscript_Postponed_Success() {
        UUID manuscriptId = UUID.randomUUID();
        UUID chiefEditorId = UUID.randomUUID();
        UUID editorId = UUID.randomUUID();

        Manuscript manuscript = new Manuscript(
                manuscriptId,
                "Аліса",
                UUID.randomUUID(),
                ManuscriptStatus.POSTPONED,
                List.of(),
                "Опис до книги Аліса",
                "url",
                Instant.now()
        );
        when(repository.findById(manuscriptId)).thenReturn(Optional.of(manuscript));

        ManuscriptApprovalRequest request = new ManuscriptApprovalRequest(editorId);
        ManuscriptResponse response = manuscriptService.approveManuscript(manuscriptId, chiefEditorId, request);
        assertNotNull(response);
        assertEquals(ManuscriptStatus.IN_PROGRESS, response.status());

        verify(repository, times(1)).save(any(Manuscript.class));
        verify(publisher, times(1)).publishEvent(any(ManuscriptApprovedEvent.class));
        verify(teamAssignmentService, times(1)).assign(manuscriptId, editorId, UserRole.EDITOR);
        verify(publisher, times(1)).publishEvent(any(WorkerAssignedEvent.class));
    }

    @Test
    void approveManuscript_InvalidStateException() {
        UUID manuscriptId = UUID.randomUUID();
        Manuscript manuscript = new Manuscript(
                manuscriptId,
                "Аліса",
                UUID.randomUUID(),
                ManuscriptStatus.IN_PROGRESS,
                List.of(),
                "Опис до книги Аліса",
                "url", Instant.now()
        );
        when(repository.findById(manuscriptId)).thenReturn(Optional.of(manuscript));
        assertThrows(InvalidStateTransitionException.class, () -> manuscriptService.approveManuscript(manuscriptId, UUID.randomUUID(), new ManuscriptApprovalRequest(UUID.randomUUID())));
    }

    @Test
    void approveManuscript_ResourceNotFoundException() {
        UUID manuscriptId = UUID.randomUUID();
        when(repository.findById(manuscriptId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> manuscriptService.approveManuscript(manuscriptId, UUID.randomUUID(), new ManuscriptApprovalRequest(UUID.randomUUID())));
        verify(repository, times(1)).findById(manuscriptId);
        verify(repository, never()).save(any());
    }

    @Test
    void rejectManuscript_Submitted_Success() {
        UUID manuscriptId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        UUID chiefEditorId = UUID.randomUUID();

        Manuscript manuscript = new Manuscript(
                manuscriptId,
                "451 градус по Фаренгейту",
                authorId,
                ManuscriptStatus.SUBMITTED,
                List.of(),
                "Опис до книги 451 градус по Фаренгейту",
                "url",
                Instant.now()
        );
        when(repository.findById(manuscriptId)).thenReturn(Optional.of(manuscript));

        ManuscriptRejectionRequest request = new ManuscriptRejectionRequest("Причина");
        ManuscriptResponse response = manuscriptService.rejectManuscript(manuscriptId, chiefEditorId, request);
        assertNotNull(response);
        assertEquals(ManuscriptStatus.REJECTED, response.status());

        verify(repository, times(1)).save(any(Manuscript.class));
        verify(publisher, times(1)).publishEvent(any(ManuscriptRejectedEvent.class));
    }

    @Test
    void rejectManuscript_Postponed_Success() {
        UUID manuscriptId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        UUID chiefEditorId = UUID.randomUUID();

        Manuscript manuscript = new Manuscript(
                manuscriptId,
                "451 градус по Фаренгейту",
                authorId,
                ManuscriptStatus.POSTPONED,
                List.of(),
                "Опис до книги 451 градус по Фаренгейту",
                "url",
                Instant.now()
        );
        when(repository.findById(manuscriptId)).thenReturn(Optional.of(manuscript));

        ManuscriptRejectionRequest request = new ManuscriptRejectionRequest("Причина");
        ManuscriptResponse response = manuscriptService.rejectManuscript(manuscriptId, chiefEditorId, request);
        assertNotNull(response);
        assertEquals(ManuscriptStatus.REJECTED, response.status());

        verify(repository, times(1)).save(any(Manuscript.class));
        verify(publisher, times(1)).publishEvent(any(ManuscriptRejectedEvent.class));
    }

    @Test
    void rejectManuscript_InvalidStateException() {
        UUID manuscriptId = UUID.randomUUID();
        Manuscript manuscript = new Manuscript(
                manuscriptId,
                "451 градус по Фаренгейту",
                UUID.randomUUID(),
                ManuscriptStatus.IN_PROGRESS,
                List.of(),
                "Опис до книги 451 градус по Фаренгейту",
                "url",
                Instant.now()
        );
        when(repository.findById(manuscriptId)).thenReturn(Optional.of(manuscript));
        assertThrows(InvalidStateTransitionException.class, () -> manuscriptService.rejectManuscript(manuscriptId, UUID.randomUUID(), new ManuscriptRejectionRequest("Причина")));
    }

    @Test
    void rejectManuscript_ResourceNotFoundException() {
        UUID manuscriptId = UUID.randomUUID();
        when(repository.findById(manuscriptId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> manuscriptService.rejectManuscript(manuscriptId, UUID.randomUUID(), new ManuscriptRejectionRequest("Причина")));
        verify(repository, times(1)).findById(manuscriptId);
        verify(repository, never()).save(any());
    }

    @Test
    void postponeManuscript_Success() {
        UUID manuscriptId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        UUID chiefEditorId = UUID.randomUUID();

        Manuscript manuscript = new Manuscript(
                manuscriptId,
                "451 градус по Фаренгейту",
                authorId,
                ManuscriptStatus.SUBMITTED,
                List.of(),
                "Опис до книги 451 градус по Фаренгейту",
                "url",
                Instant.now()
        );
        when(repository.findById(manuscriptId)).thenReturn(Optional.of(manuscript));

        ManuscriptPostponementRequest request = new ManuscriptPostponementRequest("Коментар");
        ManuscriptResponse response = manuscriptService.postponeManuscript(manuscriptId, chiefEditorId, request);
        assertNotNull(response);
        assertEquals(ManuscriptStatus.POSTPONED, response.status());

        verify(repository, times(1)).save(any(Manuscript.class));
        verify(publisher, times(1)).publishEvent(any(ManuscriptPostponedEvent.class));
    }

    @Test
    void postponeManuscript_InvalidStateException() {
        UUID manuscriptId = UUID.randomUUID();
        Manuscript manuscript = new Manuscript(
                manuscriptId,
                "451 градус по Фаренгейту",
                UUID.randomUUID(),
                ManuscriptStatus.REJECTED,
                List.of(),
                "Опис до книги 451 градус по Фаренгейту",
                "url",
                Instant.now()
        );
        when(repository.findById(manuscriptId)).thenReturn(Optional.of(manuscript));
        assertThrows(InvalidStateTransitionException.class, () -> manuscriptService.postponeManuscript(manuscriptId, UUID.randomUUID(), new ManuscriptPostponementRequest("Коментар")));
    }

    @Test
    void postponeManuscript_ResourceNotFoundException() {
        UUID manuscriptId = UUID.randomUUID();
        when(repository.findById(manuscriptId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> manuscriptService.postponeManuscript(manuscriptId, UUID.randomUUID(), new ManuscriptPostponementRequest("Коментар")));
        verify(repository, times(1)).findById(manuscriptId);
        verify(repository, never()).save(any());
        verify(publisher, never()).publishEvent(any());
    }

    @Test
    void getManuscriptById_Success() {
        UUID id = UUID.randomUUID();
        Manuscript manuscript = new Manuscript(
                id,
                "451 градус по Фаренгейту",
                UUID.randomUUID(),
                ManuscriptStatus.SUBMITTED,
                List.of(),
                "Опис до книги 451 градус по Фаренгейту",
                "url",
                Instant.now()
        );
        when(repository.findById(id)).thenReturn(Optional.of(manuscript));

        ManuscriptResponse response = manuscriptService.getManuscriptById(id);
        assertNotNull(response);
        assertEquals("451 градус по Фаренгейту", response.title());

        verify(repository, times(1)).findById(id);
    }

    @Test
    void getManuscriptsById_ResourceNotFoundException() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> manuscriptService.getManuscriptById(id));
        verify(repository, times(1)).findById(id);
    }

    @Test
    void getManuscripts_NullStatus_Success() {
        Manuscript manuscript1 = new Manuscript(
                UUID.randomUUID(),
                "Аліса",
                UUID.randomUUID(),
                ManuscriptStatus.SUBMITTED,
                List.of(),
                "Опис до книги Аліса",
                "url1",
                Instant.now()
        );
        Manuscript manuscript2 = new Manuscript(
                UUID.randomUUID(),
                "Гаррі Поттер",
                UUID.randomUUID(),
                ManuscriptStatus.IN_PROGRESS,
                List.of(),
                "Опис до книги Гаррі Поттер",
                "url2",
                Instant.now()
        );
        when(repository.findAll()).thenReturn(List.of(manuscript1, manuscript2));

        List<ManuscriptResponse> manuscripts = manuscriptService.getManuscripts(null);
        assertNotNull(manuscripts);
        assertEquals(2, manuscripts.size());
        assertEquals("Аліса", manuscripts.get(0).title());
        assertEquals("Гаррі Поттер", manuscripts.get(1).title());

        verify(repository, times(1)).findAll();
    }

    @Test
    void getManuscripts_WithStatus_Success() {
        Manuscript manuscript = new Manuscript(
                UUID.randomUUID(),
                "Аліса",
                UUID.randomUUID(),
                ManuscriptStatus.SUBMITTED,
                List.of(),
                "Опис до книги Аліса",
                "url",
                Instant.now()
        );
        when(repository.findByStatus(ManuscriptStatus.SUBMITTED)).thenReturn(List.of(manuscript));

        List<ManuscriptResponse> manuscripts = manuscriptService.getManuscripts(ManuscriptStatus.SUBMITTED);
        assertNotNull(manuscripts);
        assertEquals(1, manuscripts.size());
        assertEquals("Аліса", manuscripts.get(0).title());

        verify(repository, times(1)).findByStatus(ManuscriptStatus.SUBMITTED);
    }
}
