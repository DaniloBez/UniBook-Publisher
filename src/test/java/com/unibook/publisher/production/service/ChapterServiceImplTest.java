package com.unibook.publisher.production.service;

import com.unibook.publisher.common.enums.UserRole;
import com.unibook.publisher.common.event.RevisionAddedEvent;
import com.unibook.publisher.common.exception.business.BusinessRuleViolationException;
import com.unibook.publisher.common.exception.badrequest.InvalidFileTypeException;
import com.unibook.publisher.common.exception.business.FileUploadException;
import com.unibook.publisher.common.exception.notfound.ChapterNotFoundException;
import com.unibook.publisher.common.exception.notfound.ManuscriptNotFoundException;
import com.unibook.publisher.common.exception.notfound.RevisionNotFoundException;
import com.unibook.publisher.common.exception.security.ForbiddenActionException;
import com.unibook.publisher.common.exception.state.InvalidStateTransitionException;
import com.unibook.publisher.common.exception.storage.FileReadException;
import com.unibook.publisher.common.logging.AppLogger;
import com.unibook.publisher.production.entity.Chapter;
import com.unibook.publisher.production.entity.Manuscript;
import com.unibook.publisher.production.entity.Revision;
import com.unibook.publisher.production.entity.request.ChapterCreationRequest;
import com.unibook.publisher.production.entity.request.ChapterUpdateRequest;
import com.unibook.publisher.production.entity.request.DiffRequest;
import com.unibook.publisher.production.entity.response.ChapterResponse;
import com.unibook.publisher.production.entity.response.DiffResponse;
import com.unibook.publisher.production.entity.response.RevisionResponse;
import com.unibook.publisher.production.enums.ManuscriptStatus;
import com.unibook.publisher.production.repository.ChapterRepository;
import com.unibook.publisher.production.repository.ManuscriptRepository;
import com.unibook.publisher.production.repository.RevisionRepository;
import com.unibook.publisher.production.repository.TeamAssignmentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.io.ByteArrayInputStream;
import java.io.IOException;
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
public class ChapterServiceImplTest {

    @Mock
    private ChapterRepository chapterRepository;

    @Mock
    private ManuscriptRepository manuscriptRepository;

    @Mock
    private RevisionRepository revisionRepository;

    @Mock
    private TeamAssignmentRepository teamAssignmentRepository;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private ApplicationEventPublisher publisher;

    @Mock
    private AppLogger logger;

    @Mock
    private DiffService diffService;

    @InjectMocks
    private ChapterServiceImpl chapterService;

    private Manuscript manuscript(UUID manuscriptId, UUID authorId, ManuscriptStatus status) {
        return new Manuscript(
                manuscriptId,
                "451 градус по Фаренгейту",
                authorId,
                status,
                Set.of(),
                "Опис до книги 451 градус по Фаренгейту",
                "url",
                Instant.now()
        );
    }

    private Chapter chapter(UUID chapterId, Manuscript manuscript) {
        Chapter chapter = new Chapter(manuscript, "Розділ 1", 1);
        chapter.setChapterId(chapterId);
        return chapter;
    }

    private Revision revision(UUID revisionId, Chapter chapter, int version, String fileUrl) {
        Revision revision = new Revision(version, fileUrl, UUID.randomUUID(), Instant.now());
        revision.setRevisionId(revisionId);
        chapter.addRevision(revision);
        return revision;
    }

    @Test
    @DisplayName("Успішне створення розділу")
    void createChapter_Success() {
        UUID manuscriptId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        Manuscript manuscript = manuscript(manuscriptId, authorId, ManuscriptStatus.IN_PROGRESS);
        ChapterCreationRequest request = new ChapterCreationRequest("Розділ 1", 1);

        when(manuscriptRepository.findById(manuscriptId)).thenReturn(Optional.of(manuscript));
        when(chapterRepository.save(any(Chapter.class))).thenAnswer(i -> i.getArgument(0));
        ChapterResponse response = chapterService.createChapter(manuscriptId, authorId, request);

        assertNotNull(response);
        assertEquals("Розділ 1", response.chapterTitle());
        assertEquals(1, response.chapterIndex());
        verify(chapterRepository, times(1)).save(any(Chapter.class));
    }

    @Test
    @DisplayName("Помилка створення розділу, якщо рукопис не знайдено")
    void createChapter_ManuscriptNotFoundException() {
        UUID manuscriptId = UUID.randomUUID();

        when(manuscriptRepository.findById(manuscriptId)).thenReturn(Optional.empty());

        assertThrows(ManuscriptNotFoundException.class, () -> chapterService.createChapter(manuscriptId, UUID.randomUUID(), new ChapterCreationRequest("Розділ", 1)));
    }

    @Test
    @DisplayName("Заборонено створювати розділ користувачу, який не є автором")
    void createChapter_ForbiddenActionException() {
        UUID manuscriptId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Manuscript manuscript = manuscript(manuscriptId, authorId, ManuscriptStatus.IN_PROGRESS);

        when(manuscriptRepository.findById(manuscriptId)).thenReturn(Optional.of(manuscript));

        assertThrows(ForbiddenActionException.class, () -> chapterService.createChapter(manuscriptId, userId, new ChapterCreationRequest("Розділ", 1)));
    }

    @Test
    @DisplayName("Заборонено створювати розділ у неприпустимому статусі рукопису")
    void createChapter_InvalidStateTransitionException() {
        UUID manuscriptId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        Manuscript manuscript = manuscript(manuscriptId, authorId, ManuscriptStatus.SUBMITTED);

        when(manuscriptRepository.findById(manuscriptId)).thenReturn(Optional.of(manuscript));
        assertThrows(InvalidStateTransitionException.class, () -> chapterService.createChapter(manuscriptId, authorId, new ChapterCreationRequest("Розділ", 1)));
    }

    @Test
    @DisplayName("Успішне отримання розділів за ідентифікатором рукопису")
    void getChaptersByManuscriptId_Success() {
        UUID manuscriptId = UUID.randomUUID();
        Manuscript manuscript = manuscript(manuscriptId, UUID.randomUUID(), ManuscriptStatus.IN_PROGRESS);
        Chapter chapter = chapter(UUID.randomUUID(), manuscript);

        when(manuscriptRepository.findById(manuscriptId)).thenReturn(Optional.of(manuscript));
        when(chapterRepository.findByManuscript_ManuscriptIdOrderByChapterIndex(manuscriptId)).thenReturn(List.of(chapter));
        List<ChapterResponse> response = chapterService.getChaptersByManuscriptId(manuscriptId);

        assertNotNull(response);
        assertEquals(1, response.size());
        assertEquals("Розділ 1", response.getFirst().chapterTitle());
    }

    @Test
    @DisplayName("Помилка отримання розділів, якщо рукопис не знайдено")
    void getChaptersByManuscriptId_ManuscriptNotFoundException() {
        UUID manuscriptId = UUID.randomUUID();

        when(manuscriptRepository.findById(manuscriptId)).thenReturn(Optional.empty());

        assertThrows(ManuscriptNotFoundException.class, () -> chapterService.getChaptersByManuscriptId(manuscriptId));
    }

    @Test
    @DisplayName("Успішне завантаження ревізії призначеним редактором")
    void uploadRevision_Editor_Success() {
        UUID chapterId = UUID.randomUUID();
        UUID manuscriptId = UUID.randomUUID();
        UUID editorId = UUID.randomUUID();
        Manuscript manuscript = manuscript(manuscriptId, UUID.randomUUID(), ManuscriptStatus.IN_PROGRESS);
        InputStream fileStream = new ByteArrayInputStream("Тестовий текст розділу".getBytes(StandardCharsets.UTF_8));
        String expectedPath = manuscript.getAuthorId() + "/" + manuscriptId + "/chapters/" + chapterId + "/revisions/rev1.md";

        when(chapterRepository.findById(chapterId)).thenReturn(Optional.of(chapter(chapterId, manuscript)));
        when(manuscriptRepository.findById(manuscriptId)).thenReturn(Optional.of(manuscript));
        when(teamAssignmentRepository.existsByManuscript_ManuscriptIdAndUserIdAndRole(manuscriptId, editorId, UserRole.EDITOR)).thenReturn(true);
        when(revisionRepository.findTopByChapter_ChapterIdOrderByVersionNumberDesc(chapterId)).thenReturn(Optional.empty());
        when(fileStorageService.put(anyString(), any(InputStream.class), anyLong(), anyString())).thenReturn(expectedPath);
        when(revisionRepository.save(any(Revision.class))).thenAnswer(i -> i.getArgument(0));

        RevisionResponse response = chapterService.uploadRevision(chapterId, editorId, fileStream, 100L, "text/markdown", "chapter1.md");
        assertNotNull(response);
        assertEquals(1, response.versionNumber());
        assertEquals(expectedPath, response.fileUrl());

        verify(publisher, times(1)).publishEvent(any(RevisionAddedEvent.class));
    }

    @Test
    @DisplayName("Успішне завантаження ревізії автором")
    void uploadRevision_Author_Success() {
        UUID chapterId = UUID.randomUUID();
        UUID manuscriptId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        Manuscript manuscript = manuscript(manuscriptId, authorId, ManuscriptStatus.IN_PROGRESS);
        InputStream fileStream = new ByteArrayInputStream("Тестовий текст розділу".getBytes(StandardCharsets.UTF_8));
        String expectedPath = authorId + "/" + manuscriptId + "/chapters/" + chapterId + "/revisions/rev1.md";

        when(chapterRepository.findById(chapterId)).thenReturn(Optional.of(chapter(chapterId, manuscript)));
        when(manuscriptRepository.findById(manuscriptId)).thenReturn(Optional.of(manuscript));
        when(revisionRepository.findTopByChapter_ChapterIdOrderByVersionNumberDesc(chapterId)).thenReturn(Optional.empty());
        when(fileStorageService.put(anyString(), any(InputStream.class), anyLong(), anyString())).thenReturn(expectedPath);
        when(revisionRepository.save(any(Revision.class))).thenAnswer(i -> i.getArgument(0));

        RevisionResponse response = chapterService.uploadRevision(chapterId, authorId, fileStream, 100L, "text/markdown", "chapter1.md");
        assertNotNull(response);
        assertEquals(1, response.versionNumber());
        assertEquals(expectedPath, response.fileUrl());

        verify(publisher, times(1)).publishEvent(any(RevisionAddedEvent.class));
    }

    @Test
    @DisplayName("Автоматичне збільшення номера версії під час завантаження нової ревізії")
    void uploadRevision_Author_Success_IncrementsVersionNumber() {
        UUID chapterId = UUID.randomUUID();
        UUID manuscriptId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        Manuscript manuscript = manuscript(manuscriptId, authorId, ManuscriptStatus.IN_PROGRESS);
        Revision latestRevision = new Revision(3, "old_url", authorId, Instant.now());
        InputStream fileStream = new ByteArrayInputStream("Тестовий текст розділу".getBytes(StandardCharsets.UTF_8));
        String expectedPath = authorId + "/" + manuscriptId + "/chapters/" + chapterId + "/revisions/rev4.md";

        when(chapterRepository.findById(chapterId)).thenReturn(Optional.of(chapter(chapterId, manuscript)));
        when(manuscriptRepository.findById(manuscriptId)).thenReturn(Optional.of(manuscript));
        when(revisionRepository.findTopByChapter_ChapterIdOrderByVersionNumberDesc(chapterId)).thenReturn(Optional.of(latestRevision));
        when(fileStorageService.put(anyString(), any(InputStream.class), anyLong(), anyString())).thenReturn(expectedPath);
        when(revisionRepository.save(any(Revision.class))).thenAnswer(i -> i.getArgument(0));

        RevisionResponse response = chapterService.uploadRevision(chapterId, authorId, fileStream, 100L, "text/markdown", "chapter1.md");
        assertNotNull(response);
        assertEquals(4, response.versionNumber());
        assertEquals(expectedPath, response.fileUrl());

        verify(publisher, times(1)).publishEvent(any(RevisionAddedEvent.class));
    }

    @Test
    @DisplayName("Помилка валідації: некоректний Content-Type або null")
    void uploadRevision_InvalidContentType_ThrowsException() {
        UUID chapterId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        InputStream fileStream = new ByteArrayInputStream("Текст".getBytes(StandardCharsets.UTF_8));

        assertThrows(InvalidFileTypeException.class,
                () -> chapterService.uploadRevision(chapterId, authorId, fileStream, 100L, "application/pdf", "chapter.md"));

        assertThrows(InvalidFileTypeException.class,
                () -> chapterService.uploadRevision(chapterId, authorId, fileStream, 100L, null, "chapter.md"));
    }

    @Test
    @DisplayName("Валідація розширення файлу в extractExtension")
    void uploadRevision_InvalidFilename_ThrowsException() {
        UUID chapterId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        InputStream fileStream = new ByteArrayInputStream("Текст".getBytes(StandardCharsets.UTF_8));

        assertThrows(InvalidFileTypeException.class,
                () -> chapterService.uploadRevision(chapterId, authorId, fileStream, 100L, "text/markdown", null));

        assertThrows(InvalidFileTypeException.class,
                () -> chapterService.uploadRevision(chapterId, authorId, fileStream, 100L, "text/markdown", "filename"));

        assertThrows(InvalidFileTypeException.class,
                () -> chapterService.uploadRevision(chapterId, authorId, fileStream, 100L, "text/markdown", "filename."));
    }

    @Test
    @DisplayName("Помилка валідації: непідтримуване розширення файлу ревізії")
    void uploadRevision_InvalidExtension_ThrowsException() {
        UUID chapterId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        InputStream fileStream = new ByteArrayInputStream("Текст".getBytes(StandardCharsets.UTF_8));

        assertThrows(InvalidFileTypeException.class,
                () -> chapterService.uploadRevision(chapterId, authorId, fileStream, 100L, "text/markdown", "chapter.exe"));
    }

    @Test
    @DisplayName("Відкат завантаження у сховищі при помилці збереження в базі даних")
    void uploadRevision_RollbackOnDatabaseError() {
        UUID chapterId = UUID.randomUUID();
        UUID manuscriptId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        Manuscript manuscript = manuscript(manuscriptId, authorId, ManuscriptStatus.IN_PROGRESS);
        InputStream fileStream = new ByteArrayInputStream("Тестовий текст".getBytes(StandardCharsets.UTF_8));
        String expectedPath = authorId + "/" + manuscriptId + "/chapters/" + chapterId + "/revisions/rev1.md";

        when(chapterRepository.findById(chapterId)).thenReturn(Optional.of(chapter(chapterId, manuscript)));
        when(manuscriptRepository.findById(manuscriptId)).thenReturn(Optional.of(manuscript));
        when(revisionRepository.findTopByChapter_ChapterIdOrderByVersionNumberDesc(chapterId)).thenReturn(Optional.empty());
        when(fileStorageService.put(anyString(), any(InputStream.class), anyLong(), anyString())).thenReturn(expectedPath);
        when(revisionRepository.save(any())).thenThrow(new RuntimeException("Database error"));

        assertThrows(FileUploadException.class,
                () -> chapterService.uploadRevision(chapterId, authorId, fileStream, 100L, "text/markdown", "chapter.md"));

        verify(fileStorageService, times(1)).delete(expectedPath);
    }

    @Test
    @DisplayName("Обробка помилки при невдалій спробі відкату сховища під час завантаження ревізії")
    void uploadRevision_RollbackCleanupFails_StillThrowsUploadException() {
        UUID chapterId = UUID.randomUUID();
        UUID manuscriptId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        Manuscript manuscript = manuscript(manuscriptId, authorId, ManuscriptStatus.IN_PROGRESS);
        InputStream fileStream = new ByteArrayInputStream("Тестовий текст".getBytes(StandardCharsets.UTF_8));
        String expectedPath = authorId + "/" + manuscriptId + "/chapters/" + chapterId + "/revisions/rev1.md";

        when(chapterRepository.findById(chapterId)).thenReturn(Optional.of(chapter(chapterId, manuscript)));
        when(manuscriptRepository.findById(manuscriptId)).thenReturn(Optional.of(manuscript));
        when(revisionRepository.findTopByChapter_ChapterIdOrderByVersionNumberDesc(chapterId)).thenReturn(Optional.empty());
        when(fileStorageService.put(anyString(), any(InputStream.class), anyLong(), anyString())).thenReturn(expectedPath);
        when(revisionRepository.save(any())).thenThrow(new RuntimeException("Database error"));
        doThrow(new RuntimeException("Storage delete failed")).when(fileStorageService).delete(expectedPath);

        assertThrows(FileUploadException.class,
                () -> chapterService.uploadRevision(chapterId, authorId, fileStream, 100L, "text/markdown", "chapter.md"));

        verify(fileStorageService, times(1)).delete(expectedPath);
    }

    @Test
    @DisplayName("Заборонено завантажувати ревізію сторонньому користувачу")
    void uploadRevision_ForbiddenActionException() {
        UUID chapterId = UUID.randomUUID();
        UUID manuscriptId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Manuscript manuscript = manuscript(manuscriptId, UUID.randomUUID(), ManuscriptStatus.IN_PROGRESS);
        InputStream fileStream = new ByteArrayInputStream("Текст".getBytes(StandardCharsets.UTF_8));

        when(chapterRepository.findById(chapterId)).thenReturn(Optional.of(chapter(chapterId, manuscript)));
        when(manuscriptRepository.findById(manuscriptId)).thenReturn(Optional.of(manuscript));
        when(teamAssignmentRepository.existsByManuscript_ManuscriptIdAndUserIdAndRole(manuscriptId, userId, UserRole.EDITOR)).thenReturn(false);

        assertThrows(ForbiddenActionException.class, () -> chapterService.uploadRevision(chapterId, userId, fileStream, 10L, "text/markdown", "chapter.md"));
    }

    @Test
    @DisplayName("Помилка завантаження ревізії, якщо рукопис не знайдено")
    void uploadRevision_ManuscriptNotFoundException() {
        UUID chapterId = UUID.randomUUID();
        UUID manuscriptId = UUID.randomUUID();
        Chapter chapter = chapter(chapterId, manuscript(manuscriptId, UUID.randomUUID(), ManuscriptStatus.IN_PROGRESS));
        InputStream fileStream = new ByteArrayInputStream("Текст".getBytes(StandardCharsets.UTF_8));

        when(chapterRepository.findById(chapterId)).thenReturn(Optional.of(chapter));
        when(manuscriptRepository.findById(manuscriptId)).thenReturn(Optional.empty());

        assertThrows(ManuscriptNotFoundException.class, () -> chapterService.uploadRevision(chapterId, UUID.randomUUID(), fileStream, 10L, "text/markdown", "chapter.md"));
    }

    @Test
    @DisplayName("Помилка завантаження ревізії, якщо розділ не знайдено")
    void uploadRevision_ChapterNotFoundException() {
        UUID chapterId = UUID.randomUUID();
        InputStream fileStream = new ByteArrayInputStream("Текст".getBytes(StandardCharsets.UTF_8));

        when(chapterRepository.findById(chapterId)).thenReturn(Optional.empty());

        assertThrows(ChapterNotFoundException.class, () -> chapterService.uploadRevision(chapterId, UUID.randomUUID(), fileStream, 10L, "text/markdown", "chapter.md"));
    }

    @Test
    @DisplayName("Заборонено завантажувати ревізію у неприпустимому статусі рукопису")
    void uploadRevision_InvalidStateTransitionException() {
        UUID chapterId = UUID.randomUUID();
        UUID manuscriptId = UUID.randomUUID();
        Manuscript manuscript = manuscript(manuscriptId, UUID.randomUUID(), ManuscriptStatus.SUBMITTED);
        InputStream fileStream = new ByteArrayInputStream("Текст".getBytes(StandardCharsets.UTF_8));

        when(chapterRepository.findById(chapterId)).thenReturn(Optional.of(chapter(chapterId, manuscript)));
        when(manuscriptRepository.findById(manuscriptId)).thenReturn(Optional.of(manuscript));

        assertThrows(InvalidStateTransitionException.class, () -> chapterService.uploadRevision(chapterId, UUID.randomUUID(), fileStream, 10L, "text/markdown", "chapter.md"));
    }

    @Test
    @DisplayName("Успішне отримання списку ревізій розділу")
    void getRevisionsByChapterId_Success() {
        UUID chapterId = UUID.randomUUID();
        Chapter chapter = chapter(chapterId, manuscript(UUID.randomUUID(), UUID.randomUUID(), ManuscriptStatus.IN_PROGRESS));
        Revision revision = revision(UUID.randomUUID(), chapter, 1, "path/to/rev1.md");

        when(chapterRepository.findById(chapterId)).thenReturn(Optional.of(chapter));
        when(revisionRepository.findAllByChapter_ChapterIdOrderByVersionNumberAsc(chapterId)).thenReturn(List.of(revision));

        List<RevisionResponse> revisions = chapterService.getRevisionsByChapterId(chapterId);

        assertNotNull(revisions);
        assertEquals(1, revisions.size());
        assertEquals(1, revisions.getFirst().versionNumber());
    }

    @Test
    @DisplayName("Помилка отримання ревізій, якщо розділ не знайдено")
    void getRevisionsByChapterId_ChapterNotFoundException() {
        UUID chapterId = UUID.randomUUID();
        when(chapterRepository.findById(chapterId)).thenReturn(Optional.empty());
        assertThrows(ChapterNotFoundException.class, () -> chapterService.getRevisionsByChapterId(chapterId));
    }

    @Test
    @DisplayName("Успішне отримання порівняння (diff) між двома ревізіями розділу")
    void getDiffChapter_Success() {
        UUID chapterId = UUID.randomUUID();
        UUID fromRevisionId = UUID.randomUUID();
        UUID toRevisionId = UUID.randomUUID();

        DiffRequest request = new DiffRequest(fromRevisionId, toRevisionId);

        Chapter chapter = chapter(chapterId, manuscript(UUID.randomUUID(), UUID.randomUUID(), ManuscriptStatus.IN_PROGRESS));
        Revision fromRevision = revision(fromRevisionId, chapter, 1, "path/from.md");
        Revision toRevision = revision(toRevisionId, chapter, 2, "path/to.md");
        DiffResponse expectedResponse = new DiffResponse(fromRevisionId, toRevisionId, List.of());

        when(chapterRepository.findById(chapterId)).thenReturn(Optional.of(chapter));
        when(revisionRepository.findById(fromRevisionId)).thenReturn(Optional.of(fromRevision));
        when(revisionRepository.findById(toRevisionId)).thenReturn(Optional.of(toRevision));

        when(fileStorageService.get("path/from.md"))
                .thenReturn(new ByteArrayInputStream("Старий текст".getBytes(StandardCharsets.UTF_8)));
        when(fileStorageService.get("path/to.md"))
                .thenReturn(new ByteArrayInputStream("Новий текст".getBytes(StandardCharsets.UTF_8)));

        when(diffService.compare(fromRevisionId, toRevisionId, List.of("Старий текст"), List.of("Новий текст")))
                .thenReturn(expectedResponse);

        DiffResponse response = chapterService.getDiffChapter(chapterId, request);
        assertNotNull(response);
        assertEquals(expectedResponse, response);
        verify(diffService, times(1)).compare(fromRevisionId, toRevisionId, List.of("Старий текст"), List.of("Новий текст"));
    }

    @Test
    @DisplayName("Помилка отримання порівняння, якщо під час читання файлів ревізій виникає IOException")
    void getDiffChapter_ThrowsFileReadException_WhenInputStreamFails() {
        UUID chapterId = UUID.randomUUID();
        UUID fromRevisionId = UUID.randomUUID();
        UUID toRevisionId = UUID.randomUUID();

        DiffRequest request = new DiffRequest(fromRevisionId, toRevisionId);

        Chapter chapter = chapter(chapterId, manuscript(UUID.randomUUID(), UUID.randomUUID(), ManuscriptStatus.IN_PROGRESS));
        Revision fromRevision = revision(fromRevisionId, chapter, 1, "path/corrupted.md");
        Revision toRevision = revision(toRevisionId, chapter, 2, "path/to.md");

        when(chapterRepository.findById(chapterId)).thenReturn(Optional.of(chapter));
        when(revisionRepository.findById(fromRevisionId)).thenReturn(Optional.of(fromRevision));
        when(revisionRepository.findById(toRevisionId)).thenReturn(Optional.of(toRevision));

        InputStream failingStream = new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException("Stream corrupted");
            }
        };
        when(fileStorageService.get("path/corrupted.md")).thenReturn(failingStream);

        assertThrows(FileReadException.class, () -> chapterService.getDiffChapter(chapterId, request));
    }

    @Test
    @DisplayName("Помилка отримання порівняння, якщо розділ не знайдено")
    void getDiffChapter_ChapterNotFoundException() {
        UUID chapterId = UUID.randomUUID();
        DiffRequest request = new DiffRequest(UUID.randomUUID(), UUID.randomUUID());
        when(chapterRepository.findById(chapterId)).thenReturn(Optional.empty());
        assertThrows(ChapterNotFoundException.class, () -> chapterService.getDiffChapter(chapterId, request));
    }

    @Test
    @DisplayName("Помилка отримання порівняння, якщо початкову ревізію не знайдено")
    void getDiffChapter_FromRevisionNotFoundException() {
        UUID chapterId = UUID.randomUUID();
        UUID fromRevisionId = UUID.randomUUID();
        DiffRequest request = new DiffRequest(fromRevisionId, UUID.randomUUID());
        when(chapterRepository.findById(chapterId)).thenReturn(Optional.of(mock(Chapter.class)));
        when(revisionRepository.findById(fromRevisionId)).thenReturn(Optional.empty());
        assertThrows(RevisionNotFoundException.class, () -> chapterService.getDiffChapter(chapterId, request));
    }

    @Test
    @DisplayName("Помилка отримання порівняння, якщо кінцеву ревізію не знайдено")
    void getDiffChapter_ToRevisionNotFoundException() {
        UUID chapterId = UUID.randomUUID();
        UUID fromRevisionId = UUID.randomUUID();
        UUID toRevisionId = UUID.randomUUID();
        DiffRequest request = new DiffRequest(fromRevisionId, toRevisionId);
        when(chapterRepository.findById(chapterId)).thenReturn(Optional.of(mock(Chapter.class)));
        when(revisionRepository.findById(fromRevisionId)).thenReturn(Optional.of(mock(Revision.class)));
        when(revisionRepository.findById(toRevisionId)).thenReturn(Optional.empty());
        assertThrows(RevisionNotFoundException.class, () -> chapterService.getDiffChapter(chapterId, request));
    }

    @Test
    @DisplayName("Помилка порівняння, якщо початкова ревізія належить іншому розділу")
    void getDiffChapter_From_BusinessRuleViolationException() {
        UUID chapterId = UUID.randomUUID();
        UUID fromRevisionId = UUID.randomUUID();
        UUID toRevisionId = UUID.randomUUID();

        DiffRequest request = new DiffRequest(fromRevisionId, toRevisionId);
        Manuscript manuscript = manuscript(UUID.randomUUID(), UUID.randomUUID(), ManuscriptStatus.IN_PROGRESS);
        Chapter chapter = chapter(chapterId, manuscript);
        Chapter otherChapter = chapter(UUID.randomUUID(), manuscript);
        Revision fromRevision = revision(fromRevisionId, otherChapter, 1, "path/from.md");
        Revision toRevision = revision(toRevisionId, chapter, 2, "path/to.md");

        when(chapterRepository.findById(chapterId)).thenReturn(Optional.of(chapter));
        when(revisionRepository.findById(fromRevisionId)).thenReturn(Optional.of(fromRevision));
        when(revisionRepository.findById(toRevisionId)).thenReturn(Optional.of(toRevision));
        assertThrows(BusinessRuleViolationException.class, () -> chapterService.getDiffChapter(chapterId, request));
    }

    @Test
    @DisplayName("Помилка порівняння, якщо кінцева ревізія належить іншому розділу")
    void getDiffChapter_To_BusinessRuleViolationException() {
        UUID chapterId = UUID.randomUUID();
        UUID fromRevisionId = UUID.randomUUID();
        UUID toRevisionId = UUID.randomUUID();

        DiffRequest request = new DiffRequest(fromRevisionId, toRevisionId);
        Manuscript manuscript = manuscript(UUID.randomUUID(), UUID.randomUUID(), ManuscriptStatus.IN_PROGRESS);
        Chapter chapter = chapter(chapterId, manuscript);
        Chapter otherChapter = chapter(UUID.randomUUID(), manuscript);
        Revision fromRevision = revision(fromRevisionId, chapter, 1, "path/from.md");
        Revision toRevision = revision(toRevisionId, otherChapter, 2, "path/to.md");

        when(chapterRepository.findById(chapterId)).thenReturn(Optional.of(chapter));
        when(revisionRepository.findById(fromRevisionId)).thenReturn(Optional.of(fromRevision));
        when(revisionRepository.findById(toRevisionId)).thenReturn(Optional.of(toRevision));
        assertThrows(BusinessRuleViolationException.class, () -> chapterService.getDiffChapter(chapterId, request));
    }

    @Test
    @DisplayName("Успішне оновлення розділу автором")
    void updateChapter_Success() {
        UUID chapterId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        Chapter chapter = chapter(chapterId, manuscript(UUID.randomUUID(), authorId, ManuscriptStatus.IN_PROGRESS));

        when(chapterRepository.findById(chapterId)).thenReturn(Optional.of(chapter));
        when(chapterRepository.save(any(Chapter.class))).thenAnswer(i -> i.getArgument(0));

        ChapterResponse response = chapterService.updateChapter(chapterId, authorId, new ChapterUpdateRequest("Новий розділ", 5));

        assertNotNull(response);
        assertEquals("Новий розділ", response.chapterTitle());
        assertEquals(5, response.chapterIndex());
        assertEquals("Новий розділ", chapter.getChapterTitle());
        assertEquals(5, chapter.getChapterIndex());
        verify(chapterRepository, times(1)).save(chapter);
    }

    @Test
    @DisplayName("Помилка оновлення розділу, якщо розділ не знайдено")
    void updateChapter_ChapterNotFoundException() {
        UUID chapterId = UUID.randomUUID();
        when(chapterRepository.findById(chapterId)).thenReturn(Optional.empty());
        assertThrows(ChapterNotFoundException.class, () -> chapterService.updateChapter(chapterId, UUID.randomUUID(), new ChapterUpdateRequest("Розділ", 1)));
        verify(chapterRepository, never()).save(any());
    }

    @Test
    @DisplayName("Заборонено оновлювати розділ користувачу, який не є автором")
    void updateChapter_ForbiddenActionException() {
        UUID chapterId = UUID.randomUUID();
        Chapter chapter = chapter(chapterId, manuscript(UUID.randomUUID(), UUID.randomUUID(), ManuscriptStatus.IN_PROGRESS));
        when(chapterRepository.findById(chapterId)).thenReturn(Optional.of(chapter));
        assertThrows(ForbiddenActionException.class, () -> chapterService.updateChapter(chapterId, UUID.randomUUID(), new ChapterUpdateRequest("Розділ", 1)));
        verify(chapterRepository, never()).save(any());
    }

    @Test
    @DisplayName("Заборонено оновлювати розділ у неприпустимому статусі рукопису")
    void updateChapter_InvalidStateTransitionException() {
        UUID chapterId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        Chapter chapter = chapter(chapterId, manuscript(UUID.randomUUID(), authorId, ManuscriptStatus.SUBMITTED));
        when(chapterRepository.findById(chapterId)).thenReturn(Optional.of(chapter));
        assertThrows(InvalidStateTransitionException.class, () -> chapterService.updateChapter(chapterId, authorId, new ChapterUpdateRequest("Розділ", 1)));
        verify(chapterRepository, never()).save(any());
    }

    @Test
    @DisplayName("Успішне видалення розділу автором")
    void deleteChapter_Success() {
        UUID chapterId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        Chapter chapter = chapter(chapterId, manuscript(UUID.randomUUID(), authorId, ManuscriptStatus.IN_PROGRESS));
        when(chapterRepository.findById(chapterId)).thenReturn(Optional.of(chapter));

        chapterService.deleteChapter(chapterId, authorId);

        verify(chapterRepository, times(1)).delete(chapter);
    }

    @Test
    @DisplayName("Помилка видалення розділу, якщо розділ не знайдено")
    void deleteChapter_ChapterNotFoundException() {
        UUID chapterId = UUID.randomUUID();
        when(chapterRepository.findById(chapterId)).thenReturn(Optional.empty());
        assertThrows(ChapterNotFoundException.class, () -> chapterService.deleteChapter(chapterId, UUID.randomUUID()));
        verify(chapterRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Заборонено видаляти розділ користувачу, який не є автором")
    void deleteChapter_ForbiddenActionException() {
        UUID chapterId = UUID.randomUUID();
        Chapter chapter = chapter(chapterId, manuscript(UUID.randomUUID(), UUID.randomUUID(), ManuscriptStatus.IN_PROGRESS));
        when(chapterRepository.findById(chapterId)).thenReturn(Optional.of(chapter));
        assertThrows(ForbiddenActionException.class, () -> chapterService.deleteChapter(chapterId, UUID.randomUUID()));
        verify(chapterRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Заборонено видаляти розділ у неприпустимому статусі рукопису")
    void deleteChapter_InvalidStateTransitionException() {
        UUID chapterId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        Chapter chapter = chapter(chapterId, manuscript(UUID.randomUUID(), authorId, ManuscriptStatus.SUBMITTED));
        when(chapterRepository.findById(chapterId)).thenReturn(Optional.of(chapter));
        assertThrows(InvalidStateTransitionException.class, () -> chapterService.deleteChapter(chapterId, authorId));
        verify(chapterRepository, never()).delete(any());
    }
}