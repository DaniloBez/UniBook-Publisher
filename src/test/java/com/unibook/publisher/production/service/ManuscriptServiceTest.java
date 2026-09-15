package com.unibook.publisher.production.service;

import com.unibook.publisher.common.event.ManuscriptApprovedEvent;
import com.unibook.publisher.common.event.ManuscriptPostponedEvent;
import com.unibook.publisher.common.event.ManuscriptRejectedEvent;
import com.unibook.publisher.common.event.ManuscriptSubmittedEvent;
import com.unibook.publisher.common.exception.ResourceNotFoundException;
import com.unibook.publisher.production.entity.Manuscript;
import com.unibook.publisher.production.entity.ManuscriptStatus;
import com.unibook.publisher.production.entity.request.ManuscriptApprovalRequest;
import com.unibook.publisher.production.entity.request.ManuscriptPostponementRequest;
import com.unibook.publisher.production.entity.request.ManuscriptRejectionRequest;
import com.unibook.publisher.production.entity.request.ManuscriptSubmissionRequest;
import com.unibook.publisher.production.entity.response.ManuscriptResponse;
import com.unibook.publisher.production.repository.ManuscriptRepository;
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
public class ManuscriptServiceTest {

    @Mock
    private ManuscriptRepository repository;

    @Mock
    private ApplicationEventPublisher publisher;

    @InjectMocks
    private ManuscriptService manuscriptService;

    @Test
    @DisplayName("Успішне збереження рукопису та публікації події")
    void submitManuscript_Success() {
        ManuscriptSubmissionRequest request = new ManuscriptSubmissionRequest(
                "Гаррі Поттер",
                "Анотація до книги Гаррі Поттер",
                "url",
                List.of(UUID.randomUUID())
        );
        UUID manuscriptId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        Manuscript savedManuscript = new Manuscript(
                manuscriptId,
                request.title(),
                authorId,
                ManuscriptStatus.SUBMITTED,
                request.genreIds(),
                request.annotation(),
                request.draftFileUrl(),
                Instant.now()
        );
        when(repository.save(any(Manuscript.class))).thenReturn(savedManuscript);

        ManuscriptResponse response = manuscriptService.submitManuscript(authorId, request);
        assertNotNull(response);
        assertEquals(manuscriptId, response.manuscriptId());
        assertEquals("Гаррі Поттер", response.title());
        assertEquals(ManuscriptStatus.SUBMITTED, response.status());

        verify(repository, times(1)).save(any(Manuscript.class));
        verify(publisher, times(1)).publishEvent(any(ManuscriptSubmittedEvent.class));
    }

    @Test
    @DisplayName("Зміна статусу на IN_PROGRESS та публікація ManuscriptApprovedEvent")
    void approveManuscript_Success() {
        UUID manuscriptId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        UUID editorId = UUID.randomUUID();

        Manuscript manuscript = new Manuscript(
                manuscriptId,
                "Аліса",
                authorId,
                ManuscriptStatus.SUBMITTED,
                List.of(),
                "Опис до книги Аліса",
                "url",
                Instant.now()
        );
        when(repository.findById(manuscriptId)).thenReturn(Optional.of(manuscript));

        ManuscriptApprovalRequest request = new ManuscriptApprovalRequest(editorId);
        ManuscriptResponse response = manuscriptService.approveManuscript(manuscriptId, request);
        assertNotNull(response);
        assertEquals(ManuscriptStatus.IN_PROGRESS, response.status());

        verify(repository, times(1)).save(any(Manuscript.class));
        verify(publisher, times(1)).publishEvent(any(ManuscriptApprovedEvent.class));
    }

    @Test
    @DisplayName("Зміна статусу на REJECTED та публікація ManuscriptRejectedEvent")
    void rejectManuscript_Success() {
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
    @DisplayName("Зміна статусу на POSTPONED та публікація ManuscriptPostponedEvent")
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
    @DisplayName("Рукопис за ID")
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
    @DisplayName("Повернення списку усіх рукописів")
    void getAllManuscripts_Success() {
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

        List<ManuscriptResponse> manuscripts = manuscriptService.getAllManuscripts();
        assertNotNull(manuscripts);
        assertEquals(2, manuscripts.size());
        assertEquals("Аліса", manuscripts.get(0).title());
        assertEquals("Гаррі Поттер", manuscripts.get(1).title());

        verify(repository, times(1)).findAll();
    }

    @Test
    @DisplayName("Рукопис не знайдено")
    void manuscript_NotFound() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> manuscriptService.getManuscriptById(id));
        verify(repository, times(1)).findById(id);
    }
}
