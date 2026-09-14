package com.unibook.publisher.communication.listener;

import com.unibook.publisher.common.event.*;
import com.unibook.publisher.communication.entity.NotificationType;
import com.unibook.publisher.communication.service.NotificationService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class NotificationEventListener {
    private final NotificationService notificationService;

    public NotificationEventListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @EventListener
    public void onManuscriptSubmitted(ManuscriptSubmittedEvent event) {
        notificationService.send(
                event.authorId(),
                null,
                event.manuscriptId(),
                "Рукопис подано",
                "Ваш рукопис '" + event.manuscriptTitle() + "' успішно надіслано та передано на модерацію.",
                NotificationType.MANUSCRIPT_STATUS
        );
    }

    @EventListener
    public void onManuscriptApproved(ManuscriptApprovedEvent event) {
        notificationService.send(
                event.authorId(),
                event.editorId(),
                event.manuscriptId(),
                "Рукопис схвалено",
                "Вітаємо! Ваш рукопис '" + event.manuscriptTitle() + "' схвалено до подальшої підготовки.",
                NotificationType.MANUSCRIPT_STATUS
        );
    }

    @EventListener
    public void onManuscriptRejected(ManuscriptRejectedEvent event) {
        notificationService.send(
                event.authorId(),
                event.editorId(),
                event.manuscriptId(),
                "Рукопис відхилено",
                "Рукопис '" + event.manuscriptTitle() + "' було відхилено. Причина: " + event.reason(),
                NotificationType.MANUSCRIPT_STATUS
        );
    }

    @EventListener
    public void onManuscriptPostponed(ManuscriptPostponedEvent event) {
        notificationService.send(
                event.authorId(),
                event.editorId(),
                event.manuscriptId(),
                "Розгляд рукопису відкладено",
                "Розгляд рукопису '" + event.manuscriptTitle() + "' відкладено. Зауваження: " + event.comment(),
                NotificationType.MANUSCRIPT_STATUS
        );
    }

    @EventListener
    public void onTextFinalized(TextFinalizedEvent event) {
        notificationService.send(
                event.authorId(),
                event.editorId(),
                event.manuscriptId(),
                "Редагування тексту завершено",
                "Роботу редактора над текстом книги '" + event.manuscriptTitle() + "' офіційно завершено.",
                NotificationType.MANUSCRIPT_STATUS
        );
    }

    @EventListener
    public void onManuscriptPublished(ManuscriptPublishedEvent event) {
        notificationService.send(
                event.authorId(),
                null,
                event.manuscriptId(),
                "Книгу опубліковано!",
                "Вітаємо! Книгу '" + event.manuscriptTitle() + "' успішно опубліковано у видавництві UniBook.",
                NotificationType.MANUSCRIPT_STATUS
        );
    }

    @EventListener
    public void onContractConfirmed(ContractConfirmedEvent event) {
        notificationService.send(
                event.authorId(),
                null,
                event.manuscriptId(),
                "Контракт підтверджено",
                "Контракт на публікацію книги '" + event.manuscriptTitle() + "' успішно укладено та підтверджено.",
                NotificationType.SYSTEM
        );
    }

    @EventListener
    public void onContractRoyaltyUpdated(ContractRoyaltyUpdatedEvent event) {
        notificationService.send(
                event.authorId(),
                null,
                event.manuscriptId(),
                "Оновлення відсотка роялті",
                "Ставку роялті для книги '" + event.manuscriptTitle() + "' змінено на " + event.newRoyaltyPercent() + "%.",
                NotificationType.SYSTEM
        );
    }

    @EventListener
    public void onWorkerAssigned(WorkerAssignedEvent event) {
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

    @EventListener
    public void onCoverVersionAdded(CoverVersionAddedEvent event) {
        notificationService.send(
                event.authorId(),
                event.designerId(),
                event.manuscriptId(),
                "Нова версія обкладинки",
                "Дизайнер завантажив новий варіант обкладинки для книги '" + event.manuscriptTitle() + "'.",
                NotificationType.MANUSCRIPT_STATUS
        );
    }

    @EventListener
    public void onRevisionAdded(RevisionAddedEvent event) {
        notificationService.send(
                event.recipientId(),
                event.uploaderId(),
                event.manuscriptId(),
                "Оновлення розділу книги",
                "До розділу рукопису '" + event.manuscriptTitle() + "' завантажено нову редаговану версію.",
                NotificationType.REVIEW_FEEDBACK
        );
    }

    @EventListener
    public void onThreadOpened(ThreadOpenedEvent event) {
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

    @EventListener
    public void onThreadMessageAdded(ThreadMessageAddedEvent event) {
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
