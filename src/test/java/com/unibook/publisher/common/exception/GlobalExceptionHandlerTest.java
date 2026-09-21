package com.unibook.publisher.common.exception;

import com.unibook.publisher.common.exception.business.BusinessRuleViolationException;
import com.unibook.publisher.common.exception.conflict.DuplicateResourceException;
import com.unibook.publisher.common.exception.notfound.ResourceNotFoundException;
import com.unibook.publisher.common.exception.security.ForbiddenActionException;
import com.unibook.publisher.common.exception.security.InvalidCredentialsException;
import com.unibook.publisher.common.exception.state.InvalidStateTransitionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import tools.jackson.databind.exc.UnrecognizedPropertyException;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    @DisplayName("InvalidStateTransitionException повертає 422 з метаданими станів")
    void handleInvalidStateTransitionException() {
        UUID id = UUID.randomUUID();
        InvalidStateTransitionException ex = new InvalidStateTransitionException(
                "Manuscript", id, "IN_PROGRESS", "PUBLISHED", Set.of("TEXT_APPROVED")
        );

        ProblemDetail detail = handler.handleInvalidState(ex);

        assertEquals(HttpStatus.UNPROCESSABLE_CONTENT.value(), detail.getStatus());
        assertEquals("Недопустимий перехід між станами", detail.getTitle());
        assert detail.getProperties() != null;
        assertEquals("Manuscript", detail.getProperties().get("entityName"));
        assertEquals(id, detail.getProperties().get("entityId"));
        assertEquals("IN_PROGRESS", detail.getProperties().get("currentStatus"));
        assertEquals("PUBLISHED", detail.getProperties().get("targetStatus"));
        assertEquals(Set.of("TEXT_APPROVED"), detail.getProperties().get("allowedTransitions"));
    }

    @Test
    @DisplayName("ResourceNotFoundException повертає 404 NOT_FOUND")
    void handleResourceNotFoundException() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Рукопис", UUID.randomUUID());

        ProblemDetail detail = handler.handleNotFound(ex);

        assertEquals(HttpStatus.NOT_FOUND.value(), detail.getStatus());
        assertEquals("Ресурс не знайдено", detail.getTitle());
    }

    @Test
    @DisplayName("BusinessRuleViolationException повертає 422 UNPROCESSABLE_CONTENT")
    void handleBusinessRuleViolationException() {
        BusinessRuleViolationException ex = new BusinessRuleViolationException("Порушення бізнес-правила");

        ProblemDetail detail = handler.handleBusinessRuleViolation(ex);

        assertEquals(HttpStatus.UNPROCESSABLE_CONTENT.value(), detail.getStatus());
        assertEquals("Порушення бізнес-правила", detail.getTitle());
    }

    @Test
    @DisplayName("DuplicateResourceException повертає 409 CONFLICT")
    void handleDuplicateResourceException() {
        DuplicateResourceException ex = new DuplicateResourceException("Дублікат");

        ProblemDetail detail = handler.handleDuplicateResource(ex);

        assertEquals(HttpStatus.CONFLICT.value(), detail.getStatus());
        assertEquals("Конфлікт даних", detail.getTitle());
    }

    @Test
    @DisplayName("ForbiddenActionException повертає 403 FORBIDDEN")
    void handleForbiddenActionException() {
        ForbiddenActionException ex = new ForbiddenActionException("Дія заборонена");

        ProblemDetail detail = handler.handleForbidden(ex);

        assertEquals(HttpStatus.FORBIDDEN.value(), detail.getStatus());
        assertEquals("Заборонена дія", detail.getTitle());
    }

    @Test
    @DisplayName("InvalidCredentialsException повертає 401 UNAUTHORIZED")
    void handleInvalidCredentialsException() {
        InvalidCredentialsException ex = new InvalidCredentialsException();

        ProblemDetail detail = handler.handleInvalidCredentials(ex);

        assertEquals(HttpStatus.UNAUTHORIZED.value(), detail.getStatus());
        assertEquals("Помилка автентифікації", detail.getTitle());
    }

    @Test
    @DisplayName("Загальний DomainException повертає 400 BAD_REQUEST з заголовком 'Доменна помилка'")
    void handleDomainException() {
        DomainException ex = new DomainException("Помилка бізнес-домену") {};

        ProblemDetail detail = handler.handleDomainException(ex);

        assertEquals(HttpStatus.BAD_REQUEST.value(), detail.getStatus());
        assertEquals("Доменна помилка", detail.getTitle());
        assertEquals("Помилка бізнес-домену", detail.getDetail());
        assertNotNull(detail.getProperties().get("timestamp"));
    }

    @Test
    @DisplayName("IllegalArgumentException повертає 400 BAD_REQUEST")
    void handleIllegalArgumentException() {
        IllegalArgumentException ex = new IllegalArgumentException("Некоректний аргумент");

        ProblemDetail detail = handler.handleIllegalArgumentException(ex);

        assertEquals(HttpStatus.BAD_REQUEST.value(), detail.getStatus());
        assertEquals("Некоректний аргумент", detail.getTitle());
        assertEquals("Некоректний аргумент", detail.getDetail());
    }

    @Test
    @DisplayName("MethodArgumentNotValidException повертає 400 з картою помилок валідації по полях")
    void handleValidation() {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "target");
        bindingResult.addError(new FieldError("target", "email", "Пошта не може бути порожньою"));
        bindingResult.addError(new FieldError("target", "email", "Некоректний формат пошти"));
        bindingResult.addError(new FieldError("target", "title", "Заголовок обов'язковий"));

        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);

        ProblemDetail detail = handler.handleValidation(ex);

        assertEquals(HttpStatus.BAD_REQUEST.value(), detail.getStatus());
        assertEquals("Помилка валідації вхідних даних", detail.getDetail());
        assertNotNull(detail.getProperties());
        @SuppressWarnings("unchecked")
        Map<String, List<String>> errors = (Map<String, List<String>>) detail.getProperties().get("errors");
        assertNotNull(errors);
        assertEquals(2, errors.get("email").size());
        assertTrue(errors.get("email").contains("Пошта не може бути порожньою"));
        assertTrue(errors.get("title").contains("Заголовок обов'язковий"));
    }

    @Test
    @DisplayName("HttpMessageNotReadableException з порожнім тілом повертає 400 та відповідне повідомлення")
    void handleInvalidJson_EmptyBody() {
        HttpMessageNotReadableException ex = new HttpMessageNotReadableException("Empty body", (HttpInputMessage) null);

        ProblemDetail detail = handler.handleInvalidJson(ex);

        assertEquals(HttpStatus.BAD_REQUEST.value(), detail.getStatus());
        assertEquals("Malformed JSON Request", detail.getTitle());
        assertEquals("Тіло HTTP-запиту порожнє або відсутнє", detail.getDetail());
    }

    @Test
    @DisplayName("HttpMessageNotReadableException з невідомим полем повертає інформацію про поле")
    void handleInvalidJson_UnrecognizedProperty() {
        UnrecognizedPropertyException cause = mock(UnrecognizedPropertyException.class);
        when(cause.getPropertyName()).thenReturn("unknownField");

        HttpMessageNotReadableException ex = new HttpMessageNotReadableException("Unknown property", cause, (HttpInputMessage) null);

        ProblemDetail detail = handler.handleInvalidJson(ex);

        assertEquals(HttpStatus.BAD_REQUEST.value(), detail.getStatus());
        assertEquals("Передано невідоме поле: 'unknownField'", detail.getDetail());
    }
}
