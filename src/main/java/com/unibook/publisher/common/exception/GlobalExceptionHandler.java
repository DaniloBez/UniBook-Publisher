package com.unibook.publisher.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import tools.jackson.databind.exc.InvalidFormatException;
import tools.jackson.databind.exc.MismatchedInputException;
import tools.jackson.databind.exc.UnrecognizedPropertyException;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class GlobalExceptionHandler {
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
        detail.setProperty("timestamp", Instant.now());
        return detail;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleInvalidJson(HttpMessageNotReadableException exception) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Некоректний синтаксис або формат JSON-запиту"
        );

        detail.setTitle("Malformed JSON Request");
        detail.setProperty("timestamp", Instant.now());

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
        detail.setProperty("timestamp", Instant.now());
        return detail;
    }

    @ExceptionHandler(InvalidStateTransitionException.class)
    public ProblemDetail handleInvalidState(InvalidStateTransitionException exception) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());

        detail.setTitle("Недопустимий перехід між станами");
        detail.setProperty("timestamp", Instant.now());
        return detail;
    }

    @ExceptionHandler(ForbiddenActionException.class)
    public ProblemDetail handleForbidden(ForbiddenActionException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());

        problem.setTitle("Заборонена дія");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }
}
