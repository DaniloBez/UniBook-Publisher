package com.unibook.publisher.production.service;

import com.unibook.publisher.common.enums.UserRole;
import com.unibook.publisher.common.event.*;
import com.unibook.publisher.common.exception.notfound.GenreNotFoundException;
import com.unibook.publisher.common.exception.notfound.ManuscriptNotFoundException;
import com.unibook.publisher.common.exception.state.InvalidStateTransitionException;
import com.unibook.publisher.common.logging.AppLogger;
import com.unibook.publisher.common.security.AuthenticatedUser;
import com.unibook.publisher.identity.api.UserLookupService;
import com.unibook.publisher.identity.api.UserSummary;
import com.unibook.publisher.production.entity.Genre;
import com.unibook.publisher.production.entity.Manuscript;
import com.unibook.publisher.production.entity.request.*;
import com.unibook.publisher.production.entity.response.ManuscriptResponse;
import com.unibook.publisher.production.enums.ManuscriptStatus;
import com.unibook.publisher.production.repository.GenreRepository;
import com.unibook.publisher.production.repository.ManuscriptRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.Instant;
import java.util.*;

import static com.unibook.publisher.testutil.TestUsers.asUser;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ManuscriptServiceImplTest {

    @Mock
    private ManuscriptRepository manuscriptRepository;

    @Mock
    private ApplicationEventPublisher publisher;

    @Mock
    private TeamAssignmentService teamAssignmentService;

    @Mock
    private UserLookupService userLookupService;

    @Mock
    private GenreRepository genreRepository;

    @Mock
    private AppLogger logger;

    @InjectMocks
    private ManuscriptServiceImpl manuscriptService;

    @Test
    void getMyManuscripts_StatusAuthor_Success() {
        UUID authorId = UUID.randomUUID();
        AuthenticatedUser authorUser = new AuthenticatedUser(
                authorId,
                "author@gmail.com",
                "password",
                List.of(new SimpleGrantedAuthority("ROLE_" + UserRole.AUTHOR.name()))
        );

        Manuscript manuscript1 = new Manuscript(
                UUID.randomUUID(),
                "Назва 1",
                authorId,
                ManuscriptStatus.SUBMITTED,
                Set.of(),
                "Анотація",
                "url",
                Instant.now()
        );

        Manuscript manuscript2 = new Manuscript(
                UUID.randomUUID(),
                "Назва 2",
                authorId,
                ManuscriptStatus.IN_PROGRESS,
                Set.of(),
                "Анотація",
                "url",
                Instant.now()
        );

        when(manuscriptRepository.findByAuthorIdWithGenres(authorId)).thenReturn(List.of(manuscript1, manuscript2));

        UserSummary authorSummary = new UserSummary(
                authorId,
                "Author",
                "author@gmail.com",
                UserRole.AUTHOR,
                "https://avatar.url/default.png"
        );
        when(userLookupService.findAllByIds(Set.of(authorId))).thenReturn(Map.of(authorId, authorSummary));

        List<ManuscriptResponse> response = manuscriptService.getMyManuscripts(authorUser, ManuscriptStatus.SUBMITTED);

        assertThat(response).hasSize(1);
        assertThat(response.get(0).status()).isEqualTo(ManuscriptStatus.SUBMITTED);
        assertEquals("Author", response.get(0).authorDisplayName());

        verify(manuscriptRepository, times(1)).findByAuthorIdWithGenres(authorId);
        verify(manuscriptRepository, never()).findByAssignedUserIdWithGenres(any());
        verify(manuscriptRepository, never()).findAllWithGenres();
        verify(userLookupService, times(1)).findAllByIds(Set.of(authorId));
    }

    @Test
    void getMyManuscripts_StatusEditor_Success() {
        UUID editorId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        AuthenticatedUser editorUser = new AuthenticatedUser(
                editorId,
                "editor@gmail.com",
                "password",
                List.of(new SimpleGrantedAuthority("ROLE_" + UserRole.EDITOR.name()))
        );

        Manuscript manuscript = new Manuscript(
                UUID.randomUUID(),
                "Назва",
                authorId,
                ManuscriptStatus.IN_PROGRESS,
                Set.of(),
                "Анотація",
                "url",
                Instant.now()
        );

        when(manuscriptRepository.findByAssignedUserIdWithGenres(editorId)).thenReturn(List.of(manuscript));

        UserSummary authorSummary = new UserSummary(
                authorId,
                "Author",
                "author@gmail.com",
                UserRole.AUTHOR,
                "https://avatar.url/default.png"
        );
        when(userLookupService.findAllByIds(Set.of(authorId))).thenReturn(Map.of(authorId, authorSummary));

        List<ManuscriptResponse> response = manuscriptService.getMyManuscripts(editorUser, null);

        assertThat(response).hasSize(1);
        assertEquals("Author", response.get(0).authorDisplayName());

        verify(manuscriptRepository, times(1)).findByAssignedUserIdWithGenres(editorId);
        verify(manuscriptRepository, never()).findByAuthorIdWithGenres(any());
    }

    @Test
    void getMyManuscripts_StatusDesigner_Success() {
        UUID designerId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        AuthenticatedUser editorUser = new AuthenticatedUser(
                designerId,
                "designer@gmail.com",
                "password",
                List.of(new SimpleGrantedAuthority("ROLE_" + UserRole.DESIGNER.name()))
        );

        Manuscript manuscript = new Manuscript(
                UUID.randomUUID(),
                "Назва",
                authorId,
                ManuscriptStatus.IN_DESIGN,
                Set.of(),
                "Анотація",
                "url",
                Instant.now()
        );

        when(manuscriptRepository.findByAssignedUserIdWithGenres(designerId)).thenReturn(List.of(manuscript));

        UserSummary authorSummary = new UserSummary(
                authorId,
                "Author",
                "author@gmail.com",
                UserRole.AUTHOR,
                "https://avatar.url/default.png"
        );
        when(userLookupService.findAllByIds(Set.of(authorId))).thenReturn(Map.of(authorId, authorSummary));

        List<ManuscriptResponse> response = manuscriptService.getMyManuscripts(editorUser, null);

        assertThat(response).hasSize(1);
        assertEquals("Author", response.get(0).authorDisplayName());

        verify(manuscriptRepository, times(1)).findByAssignedUserIdWithGenres(designerId);
        verify(manuscriptRepository, never()).findByAuthorIdWithGenres(any());
    }

    @Test
    void getMyManuscripts_ChiefEditor_Success() {
        UUID chiefEditorId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        AuthenticatedUser chiefEditorUser = new AuthenticatedUser(
                chiefEditorId,
                "chief-editor@gmail.com",
                "password",
                List.of(new SimpleGrantedAuthority("ROLE_" + UserRole.CHIEF_EDITOR.name()))
        );

        Manuscript manuscript = new Manuscript(
                UUID.randomUUID(),
                "Назва",
                authorId,
                ManuscriptStatus.SUBMITTED,
                Set.of(),
                "Анотація",
                "url",
                Instant.now()
        );

        when(manuscriptRepository.findAllWithGenres()).thenReturn(List.of(manuscript));
        when(userLookupService.findAllByIds(Set.of(authorId))).thenReturn(Collections.emptyMap());

        List<ManuscriptResponse> result = manuscriptService.getMyManuscripts(chiefEditorUser, null);

        assertThat(result).hasSize(1);
        verify(manuscriptRepository, times(1)).findAllWithGenres();
    }

    @Test
    void submitManuscript_Success() {
        UUID authorId = UUID.randomUUID();
        UUID manuscriptId = UUID.randomUUID();

        ManuscriptSubmissionRequest request = new ManuscriptSubmissionRequest(
                "Гаррі Поттер",
                "Анотація до книги Гаррі Поттер",
                "url",
                List.of(UUID.randomUUID())
        );

        Manuscript saved = new Manuscript(
                manuscriptId,
                request.title(),
                authorId,
                ManuscriptStatus.SUBMITTED,
                Set.of(),
                request.annotation(),
                request.draftFileUrl(),
                Instant.now()
        );
        when(manuscriptRepository.save(any(Manuscript.class))).thenReturn(saved);

        UserSummary authorSummary = new UserSummary(authorId, "Джоан Роулінг", "author@gmail.com", UserRole.AUTHOR, "avatar.png");
        when(userLookupService.findById(authorId)).thenReturn(Optional.of(authorSummary));

        ManuscriptResponse response = manuscriptService.submitManuscript(asUser(authorId), request);
        assertNotNull(response);
        assertEquals(manuscriptId, response.manuscriptId());
        assertEquals("Гаррі Поттер", response.title());
        assertEquals(ManuscriptStatus.SUBMITTED, response.status());
        assertEquals("Джоан Роулінг", response.authorDisplayName());

        verify(genreRepository, times(1)).findAllById(request.genreIds());
        verify(manuscriptRepository, times(1)).save(any(Manuscript.class));
        verify(publisher, times(1)).publishEvent(any(ManuscriptSubmittedEvent.class));
    }

    @Test
    void approveManuscript_Submitted_Success() {
        UUID manuscriptId = UUID.randomUUID();
        UUID chiefEditorId = UUID.randomUUID();
        UUID editorId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();

        Manuscript manuscript = new Manuscript(
                manuscriptId,
                "Аліса",
                authorId,
                ManuscriptStatus.SUBMITTED,
                Set.of(),
                "Опис до книги Аліса",
                "url",
                Instant.now()
        );
        when(manuscriptRepository.findByIdWithGenres(manuscriptId)).thenReturn(Optional.of(manuscript));
        when(manuscriptRepository.save(any(Manuscript.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserSummary authorSummary = new UserSummary(authorId, "Автор Аліси", "author@gmail.com", UserRole.AUTHOR, "avatar.png");
        when(userLookupService.findById(authorId)).thenReturn(Optional.of(authorSummary));

        ManuscriptApprovalRequest request = new ManuscriptApprovalRequest(editorId);
        ManuscriptResponse response = manuscriptService.approveManuscript(manuscriptId, asUser(chiefEditorId), request);
        assertNotNull(response);
        assertEquals(ManuscriptStatus.IN_PROGRESS, response.status());
        assertEquals("Автор Аліси", response.authorDisplayName());

        verify(manuscriptRepository, times(1)).save(any(Manuscript.class));
        verify(publisher, times(1)).publishEvent(any(ManuscriptApprovedEvent.class));
        verify(teamAssignmentService, times(1)).assign(manuscriptId, editorId, UserRole.EDITOR);
        verify(publisher, times(1)).publishEvent(any(WorkerAssignedEvent.class));
    }

    @Test
    void approveManuscript_Postponed_Success() {
        UUID manuscriptId = UUID.randomUUID();
        UUID chiefEditorId = UUID.randomUUID();
        UUID editorId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();

        Manuscript manuscript = new Manuscript(
                manuscriptId,
                "Аліса",
                authorId,
                ManuscriptStatus.POSTPONED,
                Set.of(),
                "Опис до книги Аліса",
                "url",
                Instant.now()
        );
        when(manuscriptRepository.findByIdWithGenres(manuscriptId)).thenReturn(Optional.of(manuscript));
        when(manuscriptRepository.save(any(Manuscript.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserSummary authorSummary = new UserSummary(authorId, "Автор Аліси", "author@gmail.com", UserRole.AUTHOR, "avatar.png");
        when(userLookupService.findById(authorId)).thenReturn(Optional.of(authorSummary));

        ManuscriptApprovalRequest request = new ManuscriptApprovalRequest(editorId);
        ManuscriptResponse response = manuscriptService.approveManuscript(manuscriptId, asUser(chiefEditorId), request);
        assertNotNull(response);
        assertEquals(ManuscriptStatus.IN_PROGRESS, response.status());
        assertEquals("Автор Аліси", response.authorDisplayName());

        verify(manuscriptRepository, times(1)).save(any(Manuscript.class));
        verify(publisher, times(1)).publishEvent(any(ManuscriptApprovedEvent.class));
        verify(teamAssignmentService, times(1)).assign(manuscriptId, editorId, UserRole.EDITOR);
        verify(publisher, times(1)).publishEvent(any(WorkerAssignedEvent.class));
    }

    @Test
    void approveManuscript_InvalidStateException() {
        UUID manuscriptId = UUID.randomUUID();
        UUID randomUserId = UUID.randomUUID();
        Manuscript manuscript = new Manuscript(
                manuscriptId,
                "Аліса",
                UUID.randomUUID(),
                ManuscriptStatus.IN_PROGRESS,
                Set.of(),
                "Опис до книги Аліса",
                "url", Instant.now()
        );
        ManuscriptApprovalRequest request = new ManuscriptApprovalRequest(randomUserId);

        when(manuscriptRepository.findByIdWithGenres(manuscriptId)).thenReturn(Optional.of(manuscript));

        AuthenticatedUser randomUser = asUser(randomUserId);
        assertThrows(InvalidStateTransitionException.class, () -> manuscriptService.approveManuscript(manuscriptId, randomUser, request));

        verify(teamAssignmentService, never()).assign(any(), any(), any());
        verify(manuscriptRepository, never()).save(any());
        verify(publisher, never()).publishEvent(any());
    }

    @Test
    void approveManuscript_ResourceNotFoundException() {
        UUID manuscriptId = UUID.randomUUID();
        UUID randomUserId = UUID.randomUUID();
        ManuscriptApprovalRequest request = new ManuscriptApprovalRequest(randomUserId);

        when(manuscriptRepository.findByIdWithGenres(manuscriptId)).thenReturn(Optional.empty());

        AuthenticatedUser randomUser = asUser(randomUserId);
        assertThrows(ManuscriptNotFoundException.class, () -> manuscriptService.approveManuscript(manuscriptId, randomUser, request));

        verify(manuscriptRepository, times(1)).findByIdWithGenres(manuscriptId);
        verify(manuscriptRepository, never()).save(any());
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
                Set.of(),
                "Опис до книги 451 градус по Фаренгейту",
                "url",
                Instant.now()
        );
        when(manuscriptRepository.findByIdWithGenres(manuscriptId)).thenReturn(Optional.of(manuscript));
        when(manuscriptRepository.save(any(Manuscript.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserSummary authorSummary = new UserSummary(authorId, "author@gmail.com", "Автор", UserRole.AUTHOR, "avatar.png");
        when(userLookupService.findById(authorId)).thenReturn(Optional.of(authorSummary));

        ManuscriptRejectionRequest request = new ManuscriptRejectionRequest("Причина");
        ManuscriptResponse response = manuscriptService.rejectManuscript(manuscriptId, asUser(chiefEditorId), request);
        assertNotNull(response);
        assertEquals(ManuscriptStatus.REJECTED, response.status());

        verify(manuscriptRepository, times(1)).save(any(Manuscript.class));
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
                Set.of(),
                "Опис до книги 451 градус по Фаренгейту",
                "url",
                Instant.now()
        );
        when(manuscriptRepository.findByIdWithGenres(manuscriptId)).thenReturn(Optional.of(manuscript));
        when(manuscriptRepository.save(any(Manuscript.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserSummary authorSummary = new UserSummary(authorId, "author@gmail.com", "Автор", UserRole.AUTHOR, "avatar.png");
        when(userLookupService.findById(authorId)).thenReturn(Optional.of(authorSummary));

        ManuscriptRejectionRequest request = new ManuscriptRejectionRequest("Причина");
        ManuscriptResponse response = manuscriptService.rejectManuscript(manuscriptId, asUser(chiefEditorId), request);
        assertNotNull(response);
        assertEquals(ManuscriptStatus.REJECTED, response.status());

        verify(manuscriptRepository, times(1)).save(any(Manuscript.class));
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
                Set.of(),
                "Опис до книги 451 градус по Фаренгейту",
                "url",
                Instant.now()
        );
        UUID randomUserId = UUID.randomUUID();
        ManuscriptRejectionRequest request = new ManuscriptRejectionRequest("Причина");
        when(manuscriptRepository.findByIdWithGenres(manuscriptId)).thenReturn(Optional.of(manuscript));

        AuthenticatedUser randomUserUser = asUser(randomUserId);
        assertThrows(InvalidStateTransitionException.class, () -> manuscriptService.rejectManuscript(manuscriptId, randomUserUser, request));

        verify(manuscriptRepository, never()).save(any());
        verify(publisher, never()).publishEvent(any());
    }

    @Test
    void rejectManuscript_ResourceNotFoundException() {
        UUID manuscriptId = UUID.randomUUID();
        UUID randomUserId = UUID.randomUUID();
        ManuscriptRejectionRequest request = new ManuscriptRejectionRequest("Причина");

        when(manuscriptRepository.findByIdWithGenres(manuscriptId)).thenReturn(Optional.empty());

        AuthenticatedUser randomUserUser = asUser(randomUserId);
        assertThrows(ManuscriptNotFoundException.class, () -> manuscriptService.rejectManuscript(manuscriptId, randomUserUser, request));

        verify(manuscriptRepository, times(1)).findByIdWithGenres(manuscriptId);
        verify(manuscriptRepository, never()).save(any());
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
                Set.of(),
                "Опис до книги 451 градус по Фаренгейту",
                "url",
                Instant.now()
        );
        when(manuscriptRepository.findByIdWithGenres(manuscriptId)).thenReturn(Optional.of(manuscript));
        when(manuscriptRepository.save(any(Manuscript.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserSummary authorSummary = new UserSummary(authorId, "author@gmail.com", "Автор", UserRole.AUTHOR, "avatar.png");
        when(userLookupService.findById(authorId)).thenReturn(Optional.of(authorSummary));

        ManuscriptPostponementRequest request = new ManuscriptPostponementRequest("Коментар");
        ManuscriptResponse response = manuscriptService.postponeManuscript(manuscriptId, asUser(chiefEditorId), request);
        assertNotNull(response);
        assertEquals(ManuscriptStatus.POSTPONED, response.status());

        verify(manuscriptRepository, times(1)).save(any(Manuscript.class));
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
                Set.of(),
                "Опис до книги 451 градус по Фаренгейту",
                "url",
                Instant.now()
        );
        UUID randomUserId = UUID.randomUUID();
        ManuscriptPostponementRequest request = new ManuscriptPostponementRequest("Коментар");

        when(manuscriptRepository.findByIdWithGenres(manuscriptId)).thenReturn(Optional.of(manuscript));

        AuthenticatedUser randomUserUser = asUser(randomUserId);
        assertThrows(InvalidStateTransitionException.class, () -> manuscriptService.postponeManuscript(manuscriptId, randomUserUser, request));

        verify(manuscriptRepository, never()).save(any());
        verify(publisher, never()).publishEvent(any());
    }

    @Test
    void postponeManuscript_ResourceNotFoundException() {
        UUID manuscriptId = UUID.randomUUID();
        UUID randomUserId = UUID.randomUUID();
        ManuscriptPostponementRequest request = new ManuscriptPostponementRequest("Коментар");

        when(manuscriptRepository.findByIdWithGenres(manuscriptId)).thenReturn(Optional.empty());

        AuthenticatedUser randomUserUser = asUser(randomUserId);
        assertThrows(ManuscriptNotFoundException.class, () -> manuscriptService.postponeManuscript(manuscriptId, randomUserUser, request));

        verify(manuscriptRepository, times(1)).findByIdWithGenres(manuscriptId);
        verify(manuscriptRepository, never()).save(any());
        verify(publisher, never()).publishEvent(any());
    }

    @Test
    void getManuscriptById_Success() {
        UUID id = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        Manuscript manuscript = new Manuscript(
                id,
                "451 градус по Фаренгейту",
                authorId,
                ManuscriptStatus.SUBMITTED,
                Set.of(),
                "Опис до книги 451 градус по Фаренгейту",
                "url",
                Instant.now()
        );
        when(manuscriptRepository.findByIdWithGenres(id)).thenReturn(Optional.of(manuscript));

        UserSummary authorSummary = new UserSummary(authorId, "author@gmail.com", "Автор", UserRole.AUTHOR, "avatar.png");
        when(userLookupService.findById(authorId)).thenReturn(Optional.of(authorSummary));

        ManuscriptResponse response = manuscriptService.getManuscriptById(id);
        assertNotNull(response);
        assertEquals("451 градус по Фаренгейту", response.title());

        verify(manuscriptRepository, times(1)).findByIdWithGenres(id);
    }

    @Test
    void getManuscriptsById_ResourceNotFoundException() {
        UUID id = UUID.randomUUID();
        when(manuscriptRepository.findByIdWithGenres(id)).thenReturn(Optional.empty());
        assertThrows(ManuscriptNotFoundException.class, () -> manuscriptService.getManuscriptById(id));
        verify(manuscriptRepository, times(1)).findByIdWithGenres(id);
    }

    @Test
    void getManuscripts_NullStatus_Success() {
        UUID authorId1 = UUID.randomUUID();
        UUID authorId2 = UUID.randomUUID();
        Manuscript manuscript1 = new Manuscript(
                UUID.randomUUID(),
                "Аліса",
                authorId1,
                ManuscriptStatus.SUBMITTED,
                Set.of(),
                "Опис до книги Аліса",
                "url1",
                Instant.now()
        );
        Manuscript manuscript2 = new Manuscript(
                UUID.randomUUID(),
                "Гаррі Поттер",
                authorId2,
                ManuscriptStatus.IN_PROGRESS,
                Set.of(),
                "Опис до книги Гаррі Поттер",
                "url2",
                Instant.now()
        );
        when(manuscriptRepository.findAllWithGenres()).thenReturn(List.of(manuscript1, manuscript2));
        when(userLookupService.findAllByIds(Set.of(authorId1, authorId2))).thenReturn(Collections.emptyMap());

        List<ManuscriptResponse> manuscripts = manuscriptService.getManuscripts(null);
        assertNotNull(manuscripts);
        assertEquals(2, manuscripts.size());
        assertEquals("Аліса", manuscripts.get(0).title());
        assertEquals("Гаррі Поттер", manuscripts.get(1).title());

        verify(manuscriptRepository, times(1)).findAllWithGenres();
    }

    @Test
    void getManuscripts_WithStatus_Success() {
        UUID authorId = UUID.randomUUID();
        Manuscript manuscript = new Manuscript(
                UUID.randomUUID(),
                "Аліса",
                authorId,
                ManuscriptStatus.SUBMITTED,
                Set.of(),
                "Опис до книги Аліса",
                "url",
                Instant.now()
        );
        when(manuscriptRepository.findByStatusWithGenres(ManuscriptStatus.SUBMITTED)).thenReturn(List.of(manuscript));
        when(userLookupService.findAllByIds(Set.of(authorId))).thenReturn(Collections.emptyMap());

        List<ManuscriptResponse> manuscripts = manuscriptService.getManuscripts(ManuscriptStatus.SUBMITTED);
        assertNotNull(manuscripts);
        assertEquals(1, manuscripts.size());
        assertEquals("Аліса", manuscripts.getFirst().title());

        verify(manuscriptRepository, times(1)).findByStatusWithGenres(ManuscriptStatus.SUBMITTED);
    }

    @Test
    void getManuscriptsForUser_Author_Success() {
        UUID authorId = UUID.randomUUID();
        Manuscript manuscript = new Manuscript(
                UUID.randomUUID(),
                "Аліса",
                authorId,
                ManuscriptStatus.SUBMITTED,
                Set.of(),
                "Опис до книги Аліса",
                "url",
                Instant.now()
        );
        when(manuscriptRepository.findByAuthorIdWithGenres(authorId)).thenReturn(List.of(manuscript));
        when(userLookupService.findAllByIds(Set.of(authorId))).thenReturn(Collections.emptyMap());

        List<ManuscriptResponse> manuscripts = manuscriptService.getManuscriptsForUser(authorId, UserRole.AUTHOR);

        assertEquals(1, manuscripts.size());
        assertEquals("Аліса", manuscripts.getFirst().title());
        verify(manuscriptRepository, times(1)).findByAuthorIdWithGenres(authorId);
        verify(teamAssignmentService, never()).getManuscriptIdsForUser(any(), any());
        verify(manuscriptRepository, never()).findAllByIdWithGenres(any());
    }

    @Test
    void getManuscriptsForUser_Author_Empty() {
        UUID authorId = UUID.randomUUID();
        when(manuscriptRepository.findByAuthorIdWithGenres(authorId)).thenReturn(List.of());

        List<ManuscriptResponse> manuscripts = manuscriptService.getManuscriptsForUser(authorId, UserRole.AUTHOR);

        assertTrue(manuscripts.isEmpty());
        verify(manuscriptRepository, times(1)).findByAuthorIdWithGenres(authorId);
    }

    @Test
    void getManuscriptsForUser_Editor_Success() {
        UUID editorId = UUID.randomUUID();
        UUID manuscriptId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        Manuscript manuscript = new Manuscript(
                manuscriptId,
                "Гаррі Поттер",
                authorId,
                ManuscriptStatus.IN_PROGRESS,
                Set.of(),
                "Опис до книги Гаррі Поттер",
                "url",
                Instant.now()
        );
        List<UUID> ids = List.of(manuscriptId);
        when(teamAssignmentService.getManuscriptIdsForUser(editorId, UserRole.EDITOR)).thenReturn(ids);
        when(manuscriptRepository.findAllByIdWithGenres(ids)).thenReturn(List.of(manuscript));
        when(userLookupService.findAllByIds(Set.of(authorId))).thenReturn(Collections.emptyMap());

        List<ManuscriptResponse> manuscripts = manuscriptService.getManuscriptsForUser(editorId, UserRole.EDITOR);

        assertEquals(1, manuscripts.size());
        assertEquals(manuscriptId, manuscripts.getFirst().manuscriptId());
        verify(teamAssignmentService, times(1)).getManuscriptIdsForUser(editorId, UserRole.EDITOR);
        verify(manuscriptRepository, times(1)).findAllByIdWithGenres(ids);
        verify(manuscriptRepository, never()).findByAuthorIdWithGenres(any());
    }

    @Test
    void getManuscriptsForUser_Designer_Success() {
        UUID designerId = UUID.randomUUID();
        UUID manuscriptId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        Manuscript manuscript = new Manuscript(
                manuscriptId,
                "451 градус по Фаренгейту",
                authorId,
                ManuscriptStatus.IN_PROGRESS,
                Set.of(),
                "Опис до книги 451 градус по Фаренгейту",
                "url",
                Instant.now()
        );
        List<UUID> ids = List.of(manuscriptId);
        when(teamAssignmentService.getManuscriptIdsForUser(designerId, UserRole.DESIGNER)).thenReturn(ids);
        when(manuscriptRepository.findAllByIdWithGenres(ids)).thenReturn(List.of(manuscript));
        when(userLookupService.findAllByIds(Set.of(authorId))).thenReturn(Collections.emptyMap());

        List<ManuscriptResponse> manuscripts = manuscriptService.getManuscriptsForUser(designerId, UserRole.DESIGNER);

        assertEquals(1, manuscripts.size());
        assertEquals("451 градус по Фаренгейту", manuscripts.getFirst().title());
        verify(teamAssignmentService, times(1)).getManuscriptIdsForUser(designerId, UserRole.DESIGNER);
        verify(manuscriptRepository, times(1)).findAllByIdWithGenres(ids);
    }

    @Test
    void getManuscriptsForUser_NoAssignments_ReturnsEmpty() {
        UUID editorId = UUID.randomUUID();
        when(teamAssignmentService.getManuscriptIdsForUser(editorId, UserRole.EDITOR)).thenReturn(List.of());

        List<ManuscriptResponse> manuscripts = manuscriptService.getManuscriptsForUser(editorId, UserRole.EDITOR);

        assertTrue(manuscripts.isEmpty());
        verify(teamAssignmentService, times(1)).getManuscriptIdsForUser(editorId, UserRole.EDITOR);
        verify(manuscriptRepository, never()).findAllByIdWithGenres(any());
        verify(manuscriptRepository, never()).findByAuthorIdWithGenres(any());
    }

    @Test
    void updateManuscript_Success() {
        UUID id = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        UUID genreId1 = UUID.randomUUID();
        UUID genreId2 = UUID.randomUUID();

        Manuscript manuscript = new Manuscript(
                id,
                "Стара назва",
                authorId,
                ManuscriptStatus.SUBMITTED,
                new HashSet<>(),
                "Стара анотація",
                "old-url",
                Instant.now()
        );

        Genre genre1 = new Genre(genreId1, "Фантастика");
        Genre genre2 = new Genre(genreId2, "Детектив");

        ManuscriptUpdateRequest request = new ManuscriptUpdateRequest(
                "Нова назва",
                "Нова анотація",
                "new-url",
                Set.of(genreId1, genreId2)
        );

        when(manuscriptRepository.findByIdWithGenres(id)).thenReturn(Optional.of(manuscript));
        when(genreRepository.findAllById(request.genreIds())).thenReturn(List.of(genre1, genre2));
        when(manuscriptRepository.save(any(Manuscript.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserSummary authorSummary = new UserSummary(authorId, "author@gmail.com", "Автор", UserRole.AUTHOR, "avatar.png");
        when(userLookupService.findById(authorId)).thenReturn(Optional.of(authorSummary));

        ManuscriptResponse response = manuscriptService.updateManuscript(id, request);

        assertNotNull(response);
        assertEquals("Нова назва", response.title());
        assertEquals(2, response.genres().size());

        verify(manuscriptRepository, times(1)).findByIdWithGenres(id);
        verify(genreRepository, times(1)).findAllById(request.genreIds());
        verify(manuscriptRepository, times(1)).save(any(Manuscript.class));
    }

    @Test
    void updateManuscript_ManuscriptNotFoundException() {
        UUID id = UUID.randomUUID();
        ManuscriptUpdateRequest request = new ManuscriptUpdateRequest(
                "Нова назва",
                "Нова анотація",
                "new-url",
                Set.of(UUID.randomUUID())
        );

        when(manuscriptRepository.findByIdWithGenres(id)).thenReturn(Optional.empty());
        assertThrows(ManuscriptNotFoundException.class, () -> manuscriptService.updateManuscript(id, request));
        verify(manuscriptRepository, times(1)).findByIdWithGenres(id);
        verify(genreRepository, never()).findAllById(any());
        verify(manuscriptRepository, never()).save(any());
    }

    @Test
    void updateManuscript_GenreNotFoundException() {
        UUID id = UUID.randomUUID();
        UUID existingGenreId = UUID.randomUUID();
        UUID missingGenreId = UUID.randomUUID();

        Manuscript manuscript = new Manuscript(
                id,
                "Стара назва",
                UUID.randomUUID(),
                ManuscriptStatus.SUBMITTED,
                Set.of(),
                "Стара анотація",
                "old-url",
                Instant.now()
        );

        Genre genre = new Genre(existingGenreId, "Фантастика");
        ManuscriptUpdateRequest request = new ManuscriptUpdateRequest(
                "Нова назва",
                "Нова анотація",
                "new-url",
                Set.of(existingGenreId, missingGenreId)
        );

        when(manuscriptRepository.findByIdWithGenres(id)).thenReturn(Optional.of(manuscript));
        when(genreRepository.findAllById(request.genreIds())).thenReturn(List.of(genre));
        assertThrows(GenreNotFoundException.class, () -> manuscriptService.updateManuscript(id, request));
        verify(manuscriptRepository, times(1)).findByIdWithGenres(id);
        verify(genreRepository, times(1)).findAllById(request.genreIds());
        verify(manuscriptRepository, never()).save(any());
    }

    @Test
    void deleteManuscript_Success() {
        UUID id = UUID.randomUUID();

        Manuscript manuscript = new Manuscript(
                id,
                "Книга",
                UUID.randomUUID(),
                ManuscriptStatus.SUBMITTED,
                new HashSet<>(),
                "Анотація",
                "url",
                Instant.now()
        );

        when(manuscriptRepository.findByIdWithGenres(id)).thenReturn(Optional.of(manuscript));
        manuscriptService.deleteManuscript(id);
        verify(manuscriptRepository, times(1)).findByIdWithGenres(id);
        verify(manuscriptRepository, times(1)).save(manuscript);
        verify(manuscriptRepository, times(1)).delete(manuscript);
    }

    @Test
    void deleteManuscript_ManuscriptNotFoundException() {
        UUID id = UUID.randomUUID();
        when(manuscriptRepository.findByIdWithGenres(id)).thenReturn(Optional.empty());
        assertThrows(ManuscriptNotFoundException.class, () -> manuscriptService.deleteManuscript(id));
        verify(manuscriptRepository, times(1)).findByIdWithGenres(id);
        verify(manuscriptRepository, never()).save(any());
        verify(manuscriptRepository, never()).delete(any());
    }

    @Test
    void submitManuscript_NullGenres_Success() {
        UUID authorId = UUID.randomUUID();
        UUID manuscriptId = UUID.randomUUID();

        ManuscriptSubmissionRequest request = new ManuscriptSubmissionRequest(
                "Без жанрів",
                "Анотація",
                "url",
                null
        );

        Manuscript saved = new Manuscript(
                manuscriptId,
                request.title(),
                authorId,
                ManuscriptStatus.SUBMITTED,
                new HashSet<>(),
                request.annotation(),
                request.draftFileUrl(),
                Instant.now()
        );
        when(manuscriptRepository.save(any(Manuscript.class))).thenReturn(saved);

        UserSummary authorSummary = new UserSummary(authorId, "author@gmail.com", "Автор", UserRole.AUTHOR, "avatar.png");
        when(userLookupService.findById(authorId)).thenReturn(Optional.of(authorSummary));

        ManuscriptResponse response = manuscriptService.submitManuscript(asUser(authorId), request);

        assertNotNull(response);
        assertEquals(manuscriptId, response.manuscriptId());
        assertEquals("Без жанрів", response.title());

        verify(genreRepository, never()).findAllById(any());
        verify(manuscriptRepository, times(1)).save(any(Manuscript.class));
        verify(publisher, times(1)).publishEvent(any(ManuscriptSubmittedEvent.class));
    }

    @Test
    void submitManuscript_EmptyGenres_Success() {
        UUID authorId = UUID.randomUUID();
        UUID manuscriptId = UUID.randomUUID();

        ManuscriptSubmissionRequest request = new ManuscriptSubmissionRequest(
                "Порожній список",
                "Анотація",
                "url",
                List.of()
        );

        Manuscript saved = new Manuscript(
                manuscriptId,
                request.title(),
                authorId,
                ManuscriptStatus.SUBMITTED,
                new HashSet<>(),
                request.annotation(),
                request.draftFileUrl(),
                Instant.now()
        );
        when(manuscriptRepository.save(any(Manuscript.class))).thenReturn(saved);

        UserSummary authorSummary = new UserSummary(authorId, "author@gmail.com", "Автор", UserRole.AUTHOR, "avatar.png");
        when(userLookupService.findById(authorId)).thenReturn(Optional.of(authorSummary));

        ManuscriptResponse response = manuscriptService.submitManuscript(asUser(authorId), request);

        assertNotNull(response);
        assertEquals("Порожній список", response.title());

        verify(genreRepository, never()).findAllById(any());
        verify(manuscriptRepository, times(1)).save(any(Manuscript.class));
        verify(publisher, times(1)).publishEvent(any(ManuscriptSubmittedEvent.class));
    }
}