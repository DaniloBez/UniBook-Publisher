package com.unibook.publisher.communication.listener;

import com.unibook.publisher.common.enums.ThreadType;
import com.unibook.publisher.common.enums.UserRole;
import com.unibook.publisher.common.event.*;
import com.unibook.publisher.communication.entity.NotificationType;
import com.unibook.publisher.communication.service.NotificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationEventListenerTest {

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private NotificationEventListener listener;

    private final UUID manuscriptId = UUID.randomUUID();
    private final UUID authorId = UUID.randomUUID();
    private final UUID editorId = UUID.randomUUID();
    private final String manuscriptTitle = "Назва твору";

    @Nested
    @DisplayName("Події життєвого циклу рукописів")
    class ManuscriptLifecycleTests {

        @Test
        @DisplayName("onManuscriptSubmitted: системне сповіщення автору про подачу")
        void onManuscriptSubmitted_Success() {
            ManuscriptSubmittedEvent event = new ManuscriptSubmittedEvent(manuscriptId, manuscriptTitle, authorId);

            listener.onManuscriptSubmitted(event);

            verify(notificationService).send(
                    eq(authorId),
                    isNull(),
                    eq(manuscriptId),
                    eq("Рукопис подано"),
                    contains(manuscriptTitle),
                    eq(NotificationType.MANUSCRIPT_STATUS)
            );
        }

        @Test
        @DisplayName("onManuscriptApproved: сповіщення автору про схвалення від редактора")
        void onManuscriptApproved_Success() {
            ManuscriptApprovedEvent event = new ManuscriptApprovedEvent(manuscriptId, manuscriptTitle, editorId, authorId);

            listener.onManuscriptApproved(event);

            verify(notificationService).send(
                    eq(authorId),
                    eq(editorId),
                    eq(manuscriptId),
                    eq("Рукопис схвалено"),
                    contains(manuscriptTitle),
                    eq(NotificationType.MANUSCRIPT_STATUS)
            );
        }

        @Test
        @DisplayName("onManuscriptRejected: сповіщення автору з причиною відхилення")
        void onManuscriptRejected_Success() {
            String reason = "Не відповідає тематиці";
            ManuscriptRejectedEvent event = new ManuscriptRejectedEvent(manuscriptId, manuscriptTitle, editorId, authorId, reason);

            listener.onManuscriptRejected(event);

            verify(notificationService).send(
                    eq(authorId),
                    eq(editorId),
                    eq(manuscriptId),
                    eq("Рукопис відхилено"),
                    contains(reason),
                    eq(NotificationType.MANUSCRIPT_STATUS)
            );
        }

        @Test
        @DisplayName("onManuscriptPostponed: сповіщення автору з коментарем про відкладення")
        void onManuscriptPostponed_Success() {
            String comment = "Потрібно доопрацювати вступ";
            ManuscriptPostponedEvent event = new ManuscriptPostponedEvent(manuscriptId, manuscriptTitle, editorId, authorId, comment);

            listener.onManuscriptPostponed(event);

            verify(notificationService).send(
                    eq(authorId),
                    eq(editorId),
                    eq(manuscriptId),
                    eq("Розгляд рукопису відкладено"),
                    contains(comment),
                    eq(NotificationType.MANUSCRIPT_STATUS)
            );
        }

        @Test
        @DisplayName("onTextFinalized: сповіщення автору про завершення редагування")
        void onTextFinalized_Success() {
            TextFinalizedEvent event = new TextFinalizedEvent(manuscriptId, manuscriptTitle, editorId, authorId);

            listener.onTextFinalized(event);

            verify(notificationService).send(
                    eq(authorId),
                    eq(editorId),
                    eq(manuscriptId),
                    eq("Редагування тексту завершено"),
                    contains(manuscriptTitle),
                    eq(NotificationType.MANUSCRIPT_STATUS)
            );
        }

        @Test
        @DisplayName("onManuscriptPublished: системне сповіщення про публікацію книги")
        void onManuscriptPublished_Success() {
            ManuscriptPublishedEvent event = new ManuscriptPublishedEvent(manuscriptId, manuscriptTitle, authorId);

            listener.onManuscriptPublished(event);

            verify(notificationService).send(
                    eq(authorId),
                    isNull(),
                    eq(manuscriptId),
                    eq("Книгу опубліковано!"),
                    contains(manuscriptTitle),
                    eq(NotificationType.MANUSCRIPT_STATUS)
            );
        }
    }

    @Nested
    @DisplayName("Події контрактів")
    class ContractEventsTests {

        @Test
        @DisplayName("onContractConfirmed: системне сповіщення автору про підтвердження")
        void onContractConfirmed_Success() {
            UUID contractId = UUID.randomUUID();
            ContractConfirmedEvent event = new ContractConfirmedEvent(contractId, manuscriptId, manuscriptTitle, authorId);

            listener.onContractConfirmed(event);

            verify(notificationService).send(
                    eq(authorId),
                    isNull(),
                    eq(manuscriptId),
                    eq("Контракт підтверджено"),
                    contains(manuscriptTitle),
                    eq(NotificationType.SYSTEM)
            );
        }

        @Test
        @DisplayName("onContractRoyaltyUpdated: сповіщення автору про зміну роялті")
        void onContractRoyaltyUpdated_Success() {
            UUID contractId = UUID.randomUUID();
            BigDecimal royalty = new BigDecimal("12.50");
            ContractRoyaltyUpdatedEvent event = new ContractRoyaltyUpdatedEvent(contractId, manuscriptId, manuscriptTitle, authorId, royalty);

            listener.onContractRoyaltyUpdated(event);

            verify(notificationService).send(
                    eq(authorId),
                    isNull(),
                    eq(manuscriptId),
                    eq("Оновлення відсотка роялті"),
                    contains("12.50%"),
                    eq(NotificationType.SYSTEM)
            );
        }
    }

    @Nested
    @DisplayName("Події виробництва та призначень")
    class ProductionEventsTests {

        @Test
        @DisplayName("onWorkerAssigned: надсилає сповіщення і працівнику, і автору книги")
        void onWorkerAssigned_Success() {
            UUID assignedById = UUID.randomUUID();
            UUID workerId = UUID.randomUUID();
            WorkerAssignedEvent event = new WorkerAssignedEvent(
                    manuscriptId, manuscriptTitle, assignedById, workerId, UserRole.EDITOR, authorId
            );

            listener.onWorkerAssigned(event);

            verify(notificationService).send(
                    eq(workerId),
                    eq(assignedById),
                    eq(manuscriptId),
                    eq("Нове призначення"),
                    contains("EDITOR"),
                    eq(NotificationType.ASSIGNMENT)
            );

            verify(notificationService).send(
                    eq(authorId),
                    eq(assignedById),
                    eq(manuscriptId),
                    eq("Оновлення команди проєкту"),
                    contains("EDITOR"),
                    eq(NotificationType.MANUSCRIPT_STATUS)
            );

            verify(notificationService, times(2)).send(any(), any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("onCoverVersionAdded: сповіщення автору від дизайнера")
        void onCoverVersionAdded_Success() {
            UUID designerId = UUID.randomUUID();
            UUID coverVersionId = UUID.randomUUID();
            CoverVersionAddedEvent event = new CoverVersionAddedEvent(
                    manuscriptId, manuscriptTitle, coverVersionId, designerId, authorId
            );

            listener.onCoverVersionAdded(event);

            verify(notificationService).send(
                    eq(authorId),
                    eq(designerId),
                    eq(manuscriptId),
                    eq("Нова версія обкладинки"),
                    contains(manuscriptTitle),
                    eq(NotificationType.MANUSCRIPT_STATUS)
            );
        }

        @Test
        @DisplayName("onRevisionAdded: сповіщення одержувачу про нову ревізію")
        void onRevisionAdded_Success() {
            UUID chapterId = UUID.randomUUID();
            UUID revisionId = UUID.randomUUID();
            UUID uploaderId = editorId;
            UUID recipientId = authorId;

            RevisionAddedEvent event = new RevisionAddedEvent(
                    manuscriptId,
                    manuscriptTitle,
                    chapterId,
                    revisionId,
                    uploaderId,
                    recipientId
            );

            listener.onRevisionAdded(event);

            verify(notificationService).send(
                    eq(recipientId),
                    eq(uploaderId),
                    eq(manuscriptId),
                    eq("Оновлення розділу книги"),
                    contains(manuscriptTitle),
                    eq(NotificationType.REVIEW_FEEDBACK)
            );
        }
    }

    @Nested
    @DisplayName("Події гілок обговорень та коментарів")
    class ThreadEventsTests {

        @Test
        @DisplayName("onThreadOpened: сповіщення про створення нової гілки обговорення (розділ)")
        void onThreadOpened_Chapter_Success() {
            UUID chapterId = UUID.randomUUID();
            UUID threadId = UUID.randomUUID();
            UUID initiatorId = editorId;
            UUID recipientId = authorId;

            ThreadOpenedEvent event = new ThreadOpenedEvent(
                    manuscriptId,
                    manuscriptTitle,
                    ThreadType.CHAPTER,
                    chapterId,
                    threadId,
                    initiatorId,
                    recipientId,
                    "Якесь обговорення"
            );

            listener.onThreadOpened(event);

            verify(notificationService).send(
                    eq(recipientId),
                    eq(initiatorId),
                    eq(manuscriptId),
                    eq("Нове обговорення"),
                    contains("Відкрито обговорення розділу в книзі '" + manuscriptTitle),
                    eq(NotificationType.REVIEW_FEEDBACK)
            );
        }

        @Test
        @DisplayName("onThreadOpened: сповіщення про створення обговорення обкладинки (COVER)")
        void onThreadOpened_Cover_Success() {
            UUID threadId = UUID.randomUUID();
            UUID initiatorId = editorId;
            UUID recipientId = authorId;

            ThreadOpenedEvent event = new ThreadOpenedEvent(
                    manuscriptId,
                    manuscriptTitle,
                    ThreadType.COVER,
                    null,
                    threadId,
                    initiatorId,
                    recipientId,
                    "Колірна гама"
            );

            listener.onThreadOpened(event);

            verify(notificationService).send(
                    eq(recipientId),
                    eq(initiatorId),
                    eq(manuscriptId),
                    eq("Нове обговорення"),
                    contains("Дизайнер або автор створив обговорення обкладинки для '" + manuscriptTitle),
                    eq(NotificationType.REVIEW_FEEDBACK)
            );
        }

        @Test
        @DisplayName("onThreadOpened: сповіщення про створення загального обговорення (GENERAL)")
        void onThreadOpened_General_Success() {
            UUID threadId = UUID.randomUUID();
            UUID initiatorId = authorId;
            UUID recipientId = editorId;

            ThreadOpenedEvent event = new ThreadOpenedEvent(
                    manuscriptId,
                    manuscriptTitle,
                    ThreadType.GENERAL,
                    null,
                    threadId,
                    initiatorId,
                    recipientId,
                    "Загальне питання"
            );

            listener.onThreadOpened(event);

            verify(notificationService).send(
                    eq(recipientId),
                    eq(initiatorId),
                    eq(manuscriptId),
                    eq("Нове обговорення"),
                    contains("Нове загальне обговорення щодо книги '" + manuscriptTitle),
                    eq(NotificationType.REVIEW_FEEDBACK)
            );
        }

        @Test
        @DisplayName("onThreadMessageAdded: сповіщення про нове повідомлення в гілці")
        void onThreadMessageAdded_Success() {
            UUID threadId = UUID.randomUUID();
            UUID senderId = editorId;
            UUID recipientId = authorId;
            String messageText = "Будь ласка, перегляньте правки на 5 сторінці.";

            ThreadMessageAddedEvent event = new ThreadMessageAddedEvent(
                    manuscriptId,
                    threadId,
                    "Якесь обговорення",
                    senderId,
                    recipientId,
                    messageText
            );

            listener.onThreadMessageAdded(event);

            verify(notificationService).send(
                    eq(recipientId),
                    eq(senderId),
                    eq(manuscriptId),
                    contains("Нове повідомлення в обговоренні"),
                    eq(messageText),
                    eq(NotificationType.REVIEW_FEEDBACK)
            );
        }
    }
}