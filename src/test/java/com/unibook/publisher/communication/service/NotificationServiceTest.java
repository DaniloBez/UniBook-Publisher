package com.unibook.publisher.communication.service;

import com.unibook.publisher.common.exception.notfound.NotificationNotFoundException;
import com.unibook.publisher.common.exception.security.ForbiddenActionException;
import com.unibook.publisher.communication.entity.Notification;
import com.unibook.publisher.communication.entity.NotificationType;
import com.unibook.publisher.communication.entity.response.NotificationResponse;
import com.unibook.publisher.communication.repository.NotificationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationService notificationService;

    @Nested
    @DisplayName("Відправка сповіщень")
    class SendNotificationTests {

        @Test
        @DisplayName("Успішне створення повного сповіщення")
        void send_WithSenderAndTarget_Success() {
            UUID recipientId = UUID.randomUUID();
            UUID senderId = UUID.randomUUID();
            UUID targetId = UUID.randomUUID();

            when(notificationRepository.save(any(Notification.class)))
                    .thenAnswer(invocation -> {
                        Notification notification = invocation.getArgument(0);
                        return new Notification(
                                UUID.randomUUID(),
                                notification.recipientId(),
                                notification.senderId(),
                                notification.targetId(),
                                notification.title(),
                                notification.message(),
                                notification.type(),
                                notification.isRead(),
                                notification.createdAt()
                        );
                    });

            NotificationResponse response = notificationService.send(
                    recipientId, senderId, targetId, "Заголовок", "Сповіщення", NotificationType.ASSIGNMENT
            );

            assertThat(response).isNotNull();
            assertThat(response.senderId()).isEqualTo(senderId);
            assertThat(response.targetId()).isEqualTo(targetId);
            assertThat(response.title()).isEqualTo("Заголовок");
            assertThat(response.isRead()).isFalse();

            verify(notificationRepository, times(1)).save(any(Notification.class));
        }

        @Test
        @DisplayName("Створення системного сповіщення без відправника")
        void send_SystemNotification_Success() {
            UUID recipientId = UUID.randomUUID();

            when(notificationRepository.save(any(Notification.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            NotificationResponse response = notificationService.send(
                    recipientId, 
                    "Системне сповіщення",
                    "Все окей, це просто тест",
                    NotificationType.SYSTEM
            );

            assertThat(response.senderId()).isNull();
            assertThat(response.targetId()).isNull();
            assertThat(response.title()).isEqualTo("Системне сповіщення");
        }
    }

    @Nested
    @DisplayName("Отримання списку сповіщень")
    class GetNotificationsTests {

        @Test
        @DisplayName("Повертає всі сповіщення користувача")
        void getUserNotifications_All() {
            UUID userId = UUID.randomUUID();
            Notification notification1 = new Notification(
                    UUID.randomUUID(),
                    userId,
                    null,
                    null,
                    "Заголовок1",
                    "Сповіщення1",
                    NotificationType.SYSTEM,
                    false,
                    Instant.now()
            );
            Notification notification2 = new Notification(
                    UUID.randomUUID(),
                    userId,
                    null,
                    null,
                    "Заголовок2",
                    "Сповіщення2",
                    NotificationType.SYSTEM,
                    true,
                    Instant.now()
            );

            when(notificationRepository.findByRecipientId(userId)).thenReturn(List.of(notification1, notification2));

            List<NotificationResponse> result = notificationService.getUserNotifications(userId, false);

            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("Повертає тільки непрочитані сповіщення")
        void getUserNotifications_UnreadOnly() {
            UUID userId = UUID.randomUUID();
            Notification unread = new Notification(
                    UUID.randomUUID(),
                    userId,
                    null,
                    null,
                    "Заголовок1",
                    "Сповіщення1",
                    NotificationType.SYSTEM,
                    false,
                    Instant.now()
            );
            Notification read = new Notification(
                    UUID.randomUUID(),
                    userId,
                    null,
                    null,
                    "Заголовок2",
                    "Сповіщення2",
                    NotificationType.SYSTEM,
                    true,
                    Instant.now()
            );

            when(notificationRepository.findByRecipientId(userId)).thenReturn(List.of(unread, read));

            List<NotificationResponse> result = notificationService.getUserNotifications(userId, true);

            assertThat(result).hasSize(1);
            assertThat(result.getFirst().title()).isEqualTo("Заголовок1");
            assertThat(result.getFirst().isRead()).isFalse();
        }

        @Test
        @DisplayName("Повертає всі сповіщення, коли параметр unreadOnly передано як null")
        void getUserNotifications_WhenUnreadOnlyIsNull_ReturnsAll() {
            UUID userId = UUID.randomUUID();
            Notification unread = new Notification(
                    UUID.randomUUID(), userId, null, null, "Заголовок1", "Текст1", NotificationType.SYSTEM, false, Instant.now()
            );
            Notification read = new Notification(
                    UUID.randomUUID(), userId, null, null, "Заголовок2", "Текст2", NotificationType.SYSTEM, true, Instant.now()
            );

            when(notificationRepository.findByRecipientId(userId)).thenReturn(List.of(unread, read));

            List<NotificationResponse> result = notificationService.getUserNotifications(userId, null);

            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("Повертає порожній список, якщо у користувача немає сповіщень")
        void getUserNotifications_EmptyList_ReturnsEmpty() {
            UUID userId = UUID.randomUUID();
            when(notificationRepository.findByRecipientId(userId)).thenReturn(List.of());

            List<NotificationResponse> result = notificationService.getUserNotifications(userId, false);

            assertThat(result).isNotNull();
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Відмітка про прочитання")
    class MarkAsReadTests {

        @Test
        @DisplayName("Успішно відмічає сповіщення як прочитане")
        void markAsRead_Success() {
            UUID notificationId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            Notification original = new Notification(
                    notificationId, 
                    userId, 
                    null, 
                    null, 
                    "Заголовок", 
                    "Сповіщення", 
                    NotificationType.SYSTEM, 
                    false, 
                    Instant.now()
            );

            when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(original));
            when(notificationRepository.save(any(Notification.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            NotificationResponse response = notificationService.markAsRead(notificationId, userId);

            assertThat(response.isRead()).isTrue();
            verify(notificationRepository).save(argThat(Notification::isRead));
        }

        @Test
        @DisplayName("Ідемпотентність: повторна позначка вже прочитаного сповіщення не викликає помилки")
        void markAsRead_AlreadyRead_ReturnsIdempotently() {
            UUID notificationId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            Notification alreadyRead = new Notification(
                    notificationId,
                    userId,
                    null,
                    null,
                    "Заголовок",
                    "Сповіщення",
                    NotificationType.SYSTEM,
                    true,
                    Instant.now()
            );

            when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(alreadyRead));
            when(notificationRepository.save(any(Notification.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            NotificationResponse response = notificationService.markAsRead(notificationId, userId);

            assertThat(response.isRead()).isTrue();
            verify(notificationRepository).save(any(Notification.class));
        }

        @Test
        @DisplayName("Викидає ResourceNotFoundException, якщо сповіщення відсутнє")
        void markAsRead_NotFound_ThrowsException() {
            UUID notificationId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();

            when(notificationRepository.findById(notificationId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> notificationService.markAsRead(notificationId, userId))
                    .isInstanceOf(NotificationNotFoundException.class)
                    .hasMessageContaining("не знайдено");

            verify(notificationRepository, never()).save(any());
        }

        @Test
        @DisplayName("Викидає ForbiddenActionException при спробі прочитати чуже сповіщення")
        void markAsRead_ForbiddenForOtherUser_ThrowsException() {
            UUID notificationId = UUID.randomUUID();
            UUID ownerId = UUID.randomUUID();
            UUID strangerId = UUID.randomUUID();

            Notification original = new Notification(
                    notificationId, 
                    ownerId, 
                    null, 
                    null, 
                    "Заголовок", 
                    "Сповіщення", 
                    NotificationType.SYSTEM,
                    false, 
                    Instant.now()
            );

            when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(original));

            assertThatThrownBy(() -> notificationService.markAsRead(notificationId, strangerId))
                    .isInstanceOf(ForbiddenActionException.class)
                    .hasMessage("Можна відмічати тільки свої повідомлення");

            verify(notificationRepository, never()).save(any());
        }
    }
}