package com.unibook.publisher.production.service;

import com.unibook.publisher.common.enums.UserRole;
import com.unibook.publisher.common.event.CoverVersionAddedEvent;
import com.unibook.publisher.common.exception.badrequest.InvalidFileTypeException;
import com.unibook.publisher.common.exception.business.FileUploadException;
import com.unibook.publisher.common.exception.notfound.ManuscriptNotFoundException;
import com.unibook.publisher.common.exception.security.ForbiddenActionException;
import com.unibook.publisher.common.exception.state.InvalidStateTransitionException;
import com.unibook.publisher.common.logging.AppLogger;
import com.unibook.publisher.production.entity.CoverVersion;
import com.unibook.publisher.production.entity.Manuscript;
import com.unibook.publisher.production.enums.ManuscriptStatus;
import com.unibook.publisher.production.entity.TeamAssignment;
import com.unibook.publisher.production.entity.response.CoverVersionResponse;
import com.unibook.publisher.production.repository.CoverVersionRepository;
import com.unibook.publisher.production.repository.ManuscriptRepository;
import com.unibook.publisher.production.repository.TeamAssignmentRepository;
import com.unibook.publisher.storage.FileStorageService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class CoverVersionServiceImpl implements CoverVersionService {
    private final CoverVersionRepository coverVersionRepository;
    private final ManuscriptRepository manuscriptRepository;
    private final TeamAssignmentRepository teamAssignmentRepository;
    private final FileStorageService fileStorageService;
    private final ApplicationEventPublisher publisher;
    private final AppLogger logger;

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            ".jpg", ".jpeg", ".png", ".webp"
    );

    public CoverVersionServiceImpl(
            CoverVersionRepository coverVersionRepository,
            ManuscriptRepository manuscriptRepository,
            TeamAssignmentRepository teamAssignmentRepository,
            FileStorageService fileStorageService,
            ApplicationEventPublisher publisher,
            AppLogger logger
    ) {
        this.coverVersionRepository = coverVersionRepository;
        this.manuscriptRepository = manuscriptRepository;
        this.teamAssignmentRepository = teamAssignmentRepository;
        this.fileStorageService = fileStorageService;
        this.publisher = publisher;
        this.logger = logger;
    }

    @Override
    @Transactional
    public CoverVersionResponse uploadCoverVersion(
            UUID manuscriptId,
            UUID designerId,
            InputStream fileStream,
            long size,
            String contentType,
            String originalFilename
    ) {
        validateImageFile(contentType, originalFilename);

        Manuscript manuscript = manuscriptRepository.findById(manuscriptId)
                .orElseThrow(() -> new ManuscriptNotFoundException(manuscriptId));

        TeamAssignment designerAssignment = teamAssignmentRepository.findByManuscript_ManuscriptIdAndRole(manuscriptId, UserRole.DESIGNER)
                .orElseThrow(() -> new ForbiddenActionException("Дизайнера не призначено на цей рукопис"));
        if (!designerAssignment.getUserId().equals(designerId)) {
            throw new ForbiddenActionException("Завантажувати обкладинку може лише призначений дизайнер");
        }
        if (manuscript.getStatus() != ManuscriptStatus.IN_DESIGN) {
            throw new InvalidStateTransitionException(
                    "Manuscript",
                    manuscriptId,
                    manuscript.getStatus(),
                    ManuscriptStatus.IN_DESIGN,
                    manuscript.getStatus().allowedTransitions()
            );
        }

        String extension = extractExtension(originalFilename);
        String uniqueFileName = UUID.randomUUID() + extension;
        String s3Path = String.format("%s/%s/covers/%s", manuscript.getAuthorId(), manuscriptId, uniqueFileName);

        String uploadedPath = fileStorageService.put(s3Path, fileStream, size, contentType);

        try {
            int versionNumber = coverVersionRepository
                    .findTopByManuscript_ManuscriptIdOrderByVersionNumberDesc(manuscriptId)
                    .map(cv -> cv.getVersionNumber() + 1)
                    .orElse(1);

            CoverVersion saved = coverVersionRepository.save(new CoverVersion(
                    manuscript,
                    uploadedPath,
                    designerId,
                    versionNumber,
                    Instant.now()
            ));

            logger.info(
                    "Created cover version {} version {} for manuscript {} by designer {}",
                    saved.getId(),
                    saved.getVersionNumber(),
                    manuscriptId,
                    designerId
            );

            publisher.publishEvent(new CoverVersionAddedEvent(
                    manuscript.getManuscriptId(),
                    manuscript.getTitle(),
                    saved.getId(),
                    designerId,
                    manuscript.getAuthorId()
            ));

            return CoverVersionResponse.from(saved);

        } catch (Exception e) {
            try {
                fileStorageService.delete(uploadedPath);
            } catch (Exception cleanupException) {
                System.out.println("Не вдалося видалити файл " + uploadedPath + " з MinIO під час відкату: " + cleanupException.getMessage());
            }

            throw new FileUploadException(
                    "Не вдалося зберегти версію обкладинки для рукопису " + manuscriptId, e
            );
        }
    }

    private void validateImageFile(String contentType, String originalFilename) {
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new InvalidFileTypeException(
                    "Некоректний тип файлу: " + contentType + ". Дозволені лише зображення (JPEG, PNG, WEBP)"
            );
        }

        if (originalFilename == null)
            throw new InvalidFileTypeException("Назва файлу не може бути порожньою");

        String extension = extractExtension(originalFilename).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new InvalidFileTypeException(
                    "Непідтримуване розширення файлу: " + extension + ". Дозволені: " + ALLOWED_EXTENSIONS
            );
        }
    }

    private String extractExtension(String filename) {
        int lastIndexOfDot = filename.lastIndexOf(".");
        if (lastIndexOfDot == -1 || lastIndexOfDot == filename.length() - 1)
            throw new InvalidFileTypeException("Файл не містить валідного розширення");

        return filename.substring(lastIndexOfDot);
    }

    @Override
    public List<CoverVersionResponse> getCoverVersions(UUID manuscriptId) {
        if (manuscriptRepository.findById(manuscriptId).isEmpty()) {
            throw new ManuscriptNotFoundException(manuscriptId);
        }
        return coverVersionRepository.findByManuscript_ManuscriptIdOrderByVersionNumberAsc(manuscriptId).stream()
                .map(CoverVersionResponse::from)
                .toList();
    }
}
