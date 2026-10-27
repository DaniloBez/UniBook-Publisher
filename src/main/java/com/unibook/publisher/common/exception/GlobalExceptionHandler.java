package com.unibook.publisher.common.exception;

import com.unibook.publisher.common.exception.business.BusinessRuleViolationException;
import com.unibook.publisher.common.exception.business.FileUploadException;
import com.unibook.publisher.common.exception.conflict.DuplicateResourceException;
import com.unibook.publisher.common.exception.notfound.ResourceNotFoundException;
import com.unibook.publisher.common.exception.notfound.FileNotFoundException;
import com.unibook.publisher.common.exception.security.ForbiddenActionException;
import com.unibook.publisher.common.exception.security.InvalidCredentialsException;
import com.unibook.publisher.common.exception.state.InvalidStateTransitionException;
import com.unibook.publisher.common.exception.storage.FileReadException;
import com.unibook.publisher.common.exception.storage.FileStorageException;
import com.unibook.publisher.common.logging.AppLogger;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import tools.jackson.databind.exc.InvalidFormatException;
import tools.jackson.databind.exc.MismatchedInputException;
import tools.jackson.databind.exc.UnrecognizedPropertyException;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private final AppLogger logger;

    private static final String TIMESTAMP_FIELD = "timestamp";
    private static final String PATH_FIELD = "path";

    public GlobalExceptionHandler(AppLogger logger) {
        this.logger = logger;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException exception) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "Помилка валідації вхідних даних");

        Map<String, List<String>> errors = exception.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.groupingBy(
                        FieldError::getField,
                        Collectors.mapping(
                                fieldError -> fieldError.getDefaultMessage() != null
                                        ? fieldError.getDefaultMessage()
                                        : "Некоректне значення",
                                Collectors.toList()
                        )
                ));

        detail.setProperty("errors", errors);
        detail.setProperty(TIMESTAMP_FIELD, Instant.now());
        return detail;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleInvalidJson(HttpMessageNotReadableException exception) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Некоректний синтаксис або формат JSON-запиту"
        );

        detail.setTitle("Malformed JSON Request");
        detail.setProperty(TIMESTAMP_FIELD, Instant.now());

        Throwable cause = exception.getCause();

        if (cause instanceof UnrecognizedPropertyException unrecognizedProperty)
            detail.setDetail("Передано невідоме поле: '" + unrecognizedProperty.getPropertyName() + "'");
        else if (cause instanceof InvalidFormatException invalidFormat) {
            String fieldName = invalidFormat.getPath().isEmpty() ? "поле" : invalidFormat.getPath().getFirst().getPropertyName();
            detail.setDetail("Некоректний тип значення для поля: '" + fieldName + "'");
        }
        else if (cause instanceof MismatchedInputException mismatchedInput) {
            String field = mismatchedInput.getPath().isEmpty() ? "тіло запиту" : mismatchedInput.getPath().getFirst().getPropertyName();
            detail.setDetail("Несумісна структура даних або відсутнє обов'язкове поле: '" + field + "'");
        }
        else if (cause == null || cause.getMessage() == null)
            detail.setDetail("Тіло HTTP-запиту порожнє або відсутнє");

        return detail;
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleNotFound(ResourceNotFoundException exception) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
        detail.setTitle("Ресурс не знайдено");
        detail.setProperty(TIMESTAMP_FIELD, Instant.now());
        return detail;
    }

    @ExceptionHandler(FileStorageException.class)
    public ProblemDetail handleNotFound(FileStorageException exception) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
        detail.setTitle("Файл не знайдено");
        detail.setProperty(TIMESTAMP_FIELD, Instant.now());
        return detail;
    }

    @ExceptionHandler(InvalidStateTransitionException.class)
    public ProblemDetail handleInvalidState(InvalidStateTransitionException exception) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_CONTENT, exception.getMessage());
        detail.setTitle("Недопустимий перехід між станами");

        detail.setProperty("entityName", exception.getEntityName());
        detail.setProperty("entityId", exception.getEntityId());
        detail.setProperty("currentStatus", exception.getCurrentStatus());
        detail.setProperty("targetStatus", exception.getTargetStatus());
        detail.setProperty("allowedTransitions", exception.getAllowedTransitions());

        detail.setProperty(TIMESTAMP_FIELD, Instant.now());
        return detail;
    }

    @ExceptionHandler(BusinessRuleViolationException.class)
    public ProblemDetail handleBusinessRuleViolation(BusinessRuleViolationException exception) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_CONTENT, exception.getMessage());
        detail.setTitle("Порушення бізнес-правила");
        detail.setProperty(TIMESTAMP_FIELD, Instant.now());
        return detail;
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ProblemDetail handleDuplicateResource(DuplicateResourceException exception) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
        detail.setTitle("Конфлікт даних");
        detail.setProperty(TIMESTAMP_FIELD, Instant.now());
        return detail;
    }

    @ExceptionHandler(ForbiddenActionException.class)
    public ProblemDetail handleForbidden(ForbiddenActionException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, exception.getMessage());
        problem.setTitle("Заборонена дія");
        problem.setProperty(TIMESTAMP_FIELD, Instant.now());
        return problem;
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ProblemDetail handleInvalidCredentials(InvalidCredentialsException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, exception.getMessage());
        problem.setTitle("Помилка автентифікації");
        problem.setProperty(TIMESTAMP_FIELD, Instant.now());
        return problem;
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException exception, HttpServletRequest request) {
        String detail = exception.getMessage() != null ? exception.getMessage() : "You do not have permission to access this resource";
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, detail);
        problem.setTitle("Access denied");
        problem.setType(URI.create("/api/errors/forbidden"));
        problem.setProperty(TIMESTAMP_FIELD, Instant.now().toString());
        problem.setProperty(PATH_FIELD, request.getRequestURI());
        return problem;
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ProblemDetail handleBadCredentials(BadCredentialsException exception, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        problem.setTitle("Bad credentials");
        problem.setType(URI.create("/api/errors/bad-credentials"));
        problem.setProperty(TIMESTAMP_FIELD, Instant.now().toString());
        problem.setProperty(PATH_FIELD, request.getRequestURI());
        return problem;
    }

    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail handleAuthentication(AuthenticationException exception, HttpServletRequest request) {
        String detail = exception.getMessage() != null ? exception.getMessage() : "Authentication is required to access this resource";
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, detail);
        problem.setTitle("Authentication required");
        problem.setType(URI.create("/api/errors/unauthorized"));
        problem.setProperty(TIMESTAMP_FIELD, Instant.now().toString());
        problem.setProperty(PATH_FIELD, request.getRequestURI());
        return problem;
    }

    @ExceptionHandler(FileNotFoundException.class)
    public ProblemDetail handleFileNotFound(FileNotFoundException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
        problem.setTitle("Поганий запит на завантаження файлу");
        problem.setProperty(TIMESTAMP_FIELD, Instant.now());
        return problem;
    }

    @ExceptionHandler(FileReadException.class)
    public ProblemDetail handleFileRead(FileReadException exception) {
        logger.error("Помилка читання файлу: {}", exception.getMessage(), exception);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, exception.getMessage());
        problem.setTitle("Помилка читання файлу");
        problem.setProperty(TIMESTAMP_FIELD, Instant.now());
        return problem;
    }

    @ExceptionHandler(FileUploadException.class)
    public ProblemDetail handleCoverVersionUpload(FileUploadException exception) {
        logger.warn("Помилка завантаження файлу: {}", exception.getMessage(), exception);
        String detail = exception.getCause() != null ? exception.getCause().getMessage() : exception.getMessage();
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        problem.setTitle(exception.getMessage());
        problem.setProperty(TIMESTAMP_FIELD, Instant.now());
        return problem;
    }

    @ExceptionHandler(DomainException.class)
    public ProblemDetail handleDomainException(DomainException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
        problem.setTitle("Доменна помилка");
        problem.setProperty(TIMESTAMP_FIELD, Instant.now());
        return problem;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgumentException(IllegalArgumentException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
        problem.setTitle(exception.getMessage());
        problem.setProperty(TIMESTAMP_FIELD, Instant.now());
        return problem;
    }

    @ExceptionHandler(RuntimeException.class)
    public ProblemDetail handleServerSideException(RuntimeException exception) {
        logger.error("Необроблений виняток: {}", exception.getMessage(), exception);
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, exception.getMessage());
        detail.setTitle("Помилка сервера");
        detail.setProperty(TIMESTAMP_FIELD, Instant.now());
        return detail;
    }
}
