package com.unibook.publisher.integration;

import com.unibook.publisher.communication.entity.response.NotificationResponse;
import com.unibook.publisher.identity.entity.response.AuthResponse;
import com.unibook.publisher.integration.support.ApiIntegrationTestSupport;
import com.unibook.publisher.support.IntegrationTest;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class NotificationApiIntegrationTest extends ApiIntegrationTestSupport {

    @Test
    void notifications_ShouldListMarkReadAndMarkAllRead() throws Exception {
        AuthResponse author = login(AUTHOR_2_EMAIL);
        AuthResponse chiefEditor = login(CHIEF_EDITOR_EMAIL);
        UUID genreId = createGenre(chiefEditor, "IT Genre " + UUID.randomUUID()).genreId();

        // Seed data contains notifications for author2; make the starting point deterministic.
        markAllAsRead(author);
        assertThat(unreadCount(author)).isZero();
        assertThat(unreadNotifications(author)).isEmpty();

        String firstTitle = "IT Notification Manuscript " + UUID.randomUUID();
        UUID firstManuscriptId = submitManuscript(author, firstTitle, genreId).manuscriptId();

        NotificationResponse first = awaitNotification(author, firstManuscriptId, "Рукопис подано");

        assertThat(first.targetId()).isEqualTo(firstManuscriptId);
        assertThat(first.isRead()).isFalse();
        assertThat(dbUuid("select recipient_id from notifications where id = ?", first.id()))
                .isEqualTo(AUTHOR_2_ID);
        assertThat(dbBoolean("select is_read from notifications where id = ?", first.id()))
                .isFalse();
        assertThat(unreadCount(author)).isEqualTo(1);
        assertThat(unreadNotifications(author))
                .extracting(NotificationResponse::id)
                .contains(first.id());

        mockMvc.perform(authorized(
                        patch("/api/v1/notifications/{id}/read", first.id()), chiefEditor))
                .andExpect(status().isForbidden());

        assertThat(dbBoolean("select is_read from notifications where id = ?", first.id()))
                .isFalse();

        NotificationResponse read = markAsRead(author, first.id());

        assertThat(read.isRead()).isTrue();
        assertThat(dbBoolean("select is_read from notifications where id = ?", first.id()))
                .isTrue();
        assertThat(unreadCount(author)).isZero();

        String secondTitle = "IT Notification Manuscript " + UUID.randomUUID();
        UUID secondManuscriptId = submitManuscript(author, secondTitle, genreId).manuscriptId();
        NotificationResponse second = awaitNotification(author, secondManuscriptId, "Рукопис подано");

        assertThat(unreadCount(author)).isEqualTo(1);

        markAllAsRead(author);

        assertThat(unreadCount(author)).isZero();
        assertThat(dbBoolean("select is_read from notifications where id = ?", second.id()))
                .isTrue();
        assertThat(notifications(author, false))
                .extracting(NotificationResponse::id)
                .contains(first.id(), second.id());
    }
}
