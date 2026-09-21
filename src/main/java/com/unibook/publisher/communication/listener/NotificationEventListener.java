package com.unibook.publisher.communication.listener;


import com.unibook.publisher.common.event.*;
import com.unibook.publisher.common.logging.AppLogger;
import com.unibook.publisher.communication.entity.NotificationType;
import com.unibook.publisher.communication.service.NotificationService;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component
public class NotificationEventListener {
    private final NotificationService notificationService;
    private final AppLogger logger;

    public NotificationEventListener(NotificationService notificationService, AppLogger logger) {
        this.notificationService = notificationService;
        this.logger = logger;
    }

    @ApplicationModuleListener
    public void onManuscriptSubmitted(ManuscriptSubmittedEvent event) {
        logger.info("Received event ManuscriptSubmittedEvent: manuscriptId={}, authorId={}", event.manuscriptId(), event.authorId());

        notificationService.send(
                event.authorId(),
                null,
                event.manuscriptId(),
                "Рукопис подано",
                "Ваш рукопис '" + event.manuscriptTitle() + "' успішно надіслано та передано на модерацію.",
                NotificationType.MANUSCRIPT_STATUS
        );
    }

    @ApplicationModuleListener
    public void onManuscriptApproved(ManuscriptApprovedEvent event) {
        logger.info("Received event ManuscriptApprovedEvent: manuscriptId={}, authorId={}, editorId={}", event.manuscriptId(), event.authorId(), event.editorId());

        notificationService.send(
                event.authorId(),
                event.editorId(),
                event.manuscriptId(),
                "Рукопис схвалено",
                "Вітаємо! Ваш рукопис '" + event.manuscriptTitle() + "' схвалено до подальшої підготовки.",
                NotificationType.MANUSCRIPT_STATUS
        );
    }

    @ApplicationModuleListener
    public void onManuscriptRejected(ManuscriptRejectedEvent event) {
        logger.info("Received event ManuscriptRejectedEvent: manuscriptId={}, authorId={}, editorId={}",event.manuscriptId(), event.authorId(), event.editorId());

        notificationService.send(
                event.authorId(),
                event.editorId(),
                event.manuscriptId(),
                "Рукопис відхилено",
                "Рукопис '" + event.manuscriptTitle() + "' було відхилено. Причина: " + event.reason(),
                NotificationType.MANUSCRIPT_STATUS
        );
    }

    @ApplicationModuleListener
    public void onManuscriptPostponed(ManuscriptPostponedEvent event) {
        logger.info("Received event ManuscriptPostponedEvent: manuscriptId={}, authorId={}, editorId={}", event.manuscriptId(), event.authorId(), event.editorId());

        notificationService.send(
                event.authorId(),
                event.editorId(),
                event.manuscriptId(),
                "Розгляд рукопису відкладено",
                "Розгляд рукопису '" + event.manuscriptTitle() + "' відкладено. Зауваження: " + event.comment(),
                NotificationType.MANUSCRIPT_STATUS
        );
    }

    @ApplicationModuleListener
    public void onTextFinalized(TextFinalizedEvent event) {
        logger.info("Received event TextFinalizedEvent: manuscriptId={}, authorId={}, editorId={}", event.manuscriptId(), event.authorId(), event.editorId());

        notificationService.send(
                event.authorId(),
                event.editorId(),
                event.manuscriptId(),
                "Редагування тексту завершено",
                "Роботу редактора над текстом книги '" + event.manuscriptTitle() + "' офіційно завершено.",
                NotificationType.MANUSCRIPT_STATUS
        );
    }

    @ApplicationModuleListener
    public void onManuscriptPublished(ManuscriptPublishedEvent event) {
        logger.info("Received event ManuscriptPublishedEvent: manuscriptId={}, authorId={}", event.manuscriptId(), event.authorId());

        notificationService.send(
                event.authorId(),
                null,
                event.manuscriptId(),
                "Книгу опубліковано!",
                "Вітаємо! Книгу '" + event.manuscriptTitle() + "' успішно опубліковано у видавництві UniBook.",
                NotificationType.MANUSCRIPT_STATUS
        );
    }

    @ApplicationModuleListener
    public void onContractConfirmed(ContractConfirmedEvent event) {
        logger.info("Received event ContractConfirmedEvent: contractId={}, manuscriptId={}, authorId={}", event.contractId(), event.manuscriptId(), event.authorId());

        notificationService.send(
                event.authorId(),
                null,
                event.manuscriptId(),
                "Контракт підтверджено",
                "Контракт на публікацію книги '" + event.manuscriptTitle() + "' успішно укладено та підтверджено.",
                NotificationType.SYSTEM
        );
    }

    @ApplicationModuleListener
    public void onContractRoyaltyUpdated(ContractRoyaltyUpdatedEvent event) {
        logger.info("Received event ContractRoyaltyUpdatedEvent: contractId={}, manuscriptId={}, authorId={}, newRoyaltyPercent={}", event.contractId(), event.manuscriptId(), event.authorId(), event.newRoyaltyPercent());

        notificationService.send(
                event.authorId(),
                null,
                event.manuscriptId(),
                "Оновлення відсотка роялті",
                "Ставку роялті для книги '" + event.manuscriptTitle() + "' змінено на " + event.newRoyaltyPercent() + "%.",
                NotificationType.SYSTEM
        );
    }

    @ApplicationModuleListener
    public void onWorkerAssigned(WorkerAssignedEvent event) {
        logger.info("Received event WorkerAssignedEvent: manuscriptId={}, assignedById={}, workerId={}, workerRole={}, authorId={}", event.manuscriptId(), event.assignedById(), event.workerId(), event.workerRole(), event.authorId());

        notificationService.send(
                event.workerId(),
                event.assignedById(),
                event.manuscriptId(),
                "Нове призначення",
                "Вас призначено як " + event.workerRole() + " для книги '" + event.manuscriptTitle() + "'.",
                NotificationType.ASSIGNMENT
        );

        notificationService.send(
                event.authorId(),
                event.assignedById(),
                event.manuscriptId(),
                "Оновлення команди проєкту",
                "До роботи над вашою книгою '" + event.manuscriptTitle() + "' призначено " + event.workerRole() + ".",
                NotificationType.MANUSCRIPT_STATUS
        );
    }

    @ApplicationModuleListener
    public void onCoverVersionAdded(CoverVersionAddedEvent event) {
        logger.info("Received event CoverVersionAddedEvent: manuscriptId={}, coverVersionId={}, designerId={}, authorId={}", event.manuscriptId(), event.coverVersionId(), event.designerId(), event.authorId());

        notificationService.send(
                event.authorId(),
                event.designerId(),
                event.manuscriptId(),
                "Нова версія обкладинки",
                "Дизайнер завантажив новий варіант обкладинки для книги '" + event.manuscriptTitle() + "'.",
                NotificationType.MANUSCRIPT_STATUS
        );
    }

    @ApplicationModuleListener
    public void onRevisionAdded(RevisionAddedEvent event) {
        logger.info("Received event RevisionAddedEvent: manuscriptId={}, chapterId={}, revisionId={}, uploaderId={}, recipientId={}", event.manuscriptId(), event.chapterId(), event.revisionId(), event.uploaderId(), event.recipientId());

        notificationService.send(
                event.recipientId(),
                event.uploaderId(),
                event.manuscriptId(),
                "Оновлення розділу книги",
                "До розділу рукопису '" + event.manuscriptTitle() + "' завантажено нову редаговану версію.",
                NotificationType.REVIEW_FEEDBACK
        );
    }

    @ApplicationModuleListener
    public void onThreadOpened(ThreadOpenedEvent event) {
        logger.info("Received event ThreadOpenedEvent: manuscriptId={}, threadId={}, threadType={}, initiatorId={}, recipientId={}", event.manuscriptId(), event.threadId(), event.threadType(), event.initiatorId(), event.recipientId());

        String message = switch (event.threadType()) {
            case CHAPTER -> "Відкрито обговорення розділу в книзі '" + event.manuscriptTitle();
            case COVER -> "Дизайнер або автор створив обговорення обкладинки для '" + event.manuscriptTitle();
            case GENERAL -> "Нове загальне обговорення щодо книги '" + event.manuscriptTitle();
        };

        message += "': " + event.topicTitle();

        notificationService.send(
                event.recipientId(),
                event.initiatorId(),
                event.manuscriptId(),
                "Нове обговорення",
                message,
                NotificationType.REVIEW_FEEDBACK
        );
    }

    @ApplicationModuleListener
    public void onThreadMessageAdded(ThreadMessageAddedEvent event) {
        logger.info("Received event ThreadMessageAddedEvent: manuscriptId={}, threadId={}, senderId={}, recipientUserId={}", event.manuscriptId(), event.threadId(), event.senderId(), event.recipientUserId());

        notificationService.send(
                event.recipientUserId(),
                event.senderId(),
                event.manuscriptId(),
                "Нове повідомлення в обговоренні '" + event.threadTitle() + "'.",
                event.message(),
                NotificationType.REVIEW_FEEDBACK
        );
    }
}
