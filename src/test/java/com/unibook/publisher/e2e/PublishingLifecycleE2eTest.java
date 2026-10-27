package com.unibook.publisher.e2e;

import com.unibook.publisher.common.enums.ContractStatus;
import com.unibook.publisher.common.enums.UserRole;
import com.unibook.publisher.communication.entity.NotificationType;
import com.unibook.publisher.communication.entity.response.NotificationResponse;
import com.unibook.publisher.communication.entity.response.UnreadCountResponse;
import com.unibook.publisher.finance.entity.request.RoyaltyUpdateRequest;
import com.unibook.publisher.finance.entity.response.ContractResponse;
import com.unibook.publisher.identity.entity.request.LoginRequest;
import com.unibook.publisher.identity.entity.response.AuthResponse;
import com.unibook.publisher.production.entity.request.AssignDesignerRequest;
import com.unibook.publisher.production.entity.request.ChapterCreationRequest;
import com.unibook.publisher.production.entity.request.ChapterUpdateRequest;
import com.unibook.publisher.production.entity.request.GenreRequest;
import com.unibook.publisher.production.entity.request.ManuscriptApprovalRequest;
import com.unibook.publisher.production.entity.request.ManuscriptSubmissionRequest;
import com.unibook.publisher.production.entity.response.ChapterResponse;
import com.unibook.publisher.production.entity.response.CoverVersionResponse;
import com.unibook.publisher.production.entity.response.GenreResponse;
import com.unibook.publisher.production.entity.response.ManuscriptAuditLogResponse;
import com.unibook.publisher.production.entity.response.ManuscriptResponse;
import com.unibook.publisher.production.enums.ManuscriptStatus;
import com.unibook.publisher.support.E2ETest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

//Full lifecycle
//as Spring Modulith works async, Awaitility is used instead of fixed delays
@E2ETest
class PublishingLifecycleE2eTest {

    private static final String PASSWORD = "SuperSecretPassword123!";

    private static final String CHIEF_EDITOR_EMAIL = "chief.editor@unibook-demo.local";
    private static final String EDITOR_EMAIL = "editor@unibook-demo.local";
    private static final String DESIGNER_EMAIL = "designer@unibook-demo.local";
    private static final String AUTHOR_EMAIL = "author1@unibook-demo.local";
    private static final String ACCOUNTANT_EMAIL = "accountant@unibook-demo.local";

    private static final byte[] COVER_PNG = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg=="
    );

    @Autowired
    private RestTestClient client;

    @Test
    void manuscriptShouldTravelFromSubmissionToPublicationWithNotifications() {
        //auth
        LoginResult chiefEditor = loginWithCookie(CHIEF_EDITOR_EMAIL);
        LoginResult editor = loginWithCookie(EDITOR_EMAIL);
        LoginResult designer = loginWithCookie(DESIGNER_EMAIL);
        LoginResult author = loginWithCookie(AUTHOR_EMAIL);
        LoginResult accountant = loginWithCookie(ACCOUNTANT_EMAIL);

        assertThat(chiefEditor.response.role()).isEqualTo(UserRole.CHIEF_EDITOR);
        assertThat(editor.response.role()).isEqualTo(UserRole.EDITOR);
        assertThat(designer.response.role()).isEqualTo(UserRole.DESIGNER);
        assertThat(author.response.role()).isEqualTo(UserRole.AUTHOR);
        assertThat(accountant.response.role()).isEqualTo(UserRole.ACCOUNTANT);

        AuthResponse refreshedAuthor = refresh(author.refreshTokenCookie);
        assertThat(refreshedAuthor.accessToken()).isNotBlank();

        RestTestClient chiefApi = authorized(chiefEditor.response);
        RestTestClient editorApi = authorized(editor.response);
        RestTestClient designerApi = authorized(designer.response);
        RestTestClient authorApi = authorized(refreshedAuthor);
        RestTestClient accountantApi = authorized(accountant.response);

        client.get().uri("/api/v1/genres")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody()
                .jsonPath("$.status").isEqualTo(401);

        String suffix = Long.toString(System.nanoTime());
        UUID genreId = createGenre(chiefApi, "E2E Genre " + suffix);

        //submission
        long authorUnreadBeforeSubmit = unreadCount(authorApi);
        String title = "E2E Manuscript " + suffix;

        ManuscriptResponse submitted = submitManuscript(authorApi, title, genreId);
        assertThat(submitted.manuscriptId()).isNotNull();
        assertThat(submitted.status()).isEqualTo(ManuscriptStatus.SUBMITTED);
        assertThat(submitted.authorId()).isEqualTo(author.response.userId());
        assertThat(submitted.genres()).extracting(GenreResponse::genreId).containsExactly(genreId);

        UUID manuscriptId = submitted.manuscriptId();
        awaitNotification(authorApi, manuscriptId, "Рукопис подано");
        awaitUnreadCount(authorApi, authorUnreadBeforeSubmit + 1);

        assertThat(unreadNotifications(authorApi))
                .filteredOn(notification -> manuscriptId.equals(notification.targetId())
                        && "Рукопис подано".equals(notification.title()))
                .singleElement()
                .satisfies(notification -> {
                    assertThat(notification.type()).isEqualTo(NotificationType.MANUSCRIPT_STATUS);
                    assertThat(notification.isRead()).isFalse();
                    assertThat(notification.message()).contains(title);
                });

        assertThat(manuscriptsByStatus(chiefApi, ManuscriptStatus.SUBMITTED))
                .extracting(ManuscriptResponse::manuscriptId)
                .contains(manuscriptId);

        //approving
        ManuscriptResponse approved = approveManuscript(chiefApi, manuscriptId, editor.response.userId());
        assertThat(approved.status()).isEqualTo(ManuscriptStatus.IN_PROGRESS);

        awaitNotification(authorApi, manuscriptId, "Рукопис схвалено");
        awaitNotification(authorApi, manuscriptId, "Оновлення команди проєкту");
        awaitNotification(editorApi, manuscriptId, "Нове призначення");

        //ensuring of receicing message
        assertHasUnreadNotification(editorApi, manuscriptId, "Нове призначення");

        //editorial workflow
        ChapterResponse chapter = createChapter(authorApi, manuscriptId, "Chapter 1. The Beginning", 1);
        assertThat(chapter.chapterTitle()).isEqualTo("Chapter 1. The Beginning");
        assertThat(chapter.chapterIndex()).isEqualTo(1);

        ChapterResponse updatedChapter = updateChapter(authorApi, chapter.chapterId(),
                "Chapter 1. The New Beginning", 1);
        assertThat(updatedChapter.chapterTitle()).isEqualTo("Chapter 1. The New Beginning");

        ManuscriptResponse textApproved = finalizeText(editorApi, manuscriptId);
        assertThat(textApproved.status()).isEqualTo(ManuscriptStatus.TEXT_APPROVED);
        awaitNotification(authorApi, manuscriptId, "Редагування тексту завершено");

        //assigning designer
        ManuscriptResponse inDesign = assignDesigner(chiefApi, manuscriptId, designer.response.userId());
        assertThat(inDesign.status()).isEqualTo(ManuscriptStatus.IN_DESIGN);

        awaitNotification(designerApi, manuscriptId, "Нове призначення");
        awaitNotification(authorApi, manuscriptId, "Оновлення команди проєкту");
        assertHasUnreadNotification(designerApi, manuscriptId, "Нове призначення");

        //uploading cover
        CoverVersionResponse cover = uploadCover(designerApi, manuscriptId);
        assertThat(cover.versionNumber()).isEqualTo(1);
        assertThat(cover.uploadedByUserId()).isEqualTo(designer.response.userId());
        assertThat(cover.isApproved()).isFalse();

        awaitNotification(authorApi, manuscriptId, "Нова версія обкладинки");
        assertHasUnreadNotification(authorApi, manuscriptId, "Нова версія обкладинки");

        //check contract
        ContractResponse contract = awaitContract(authorApi, manuscriptId);
        assertThat(contract.status()).isEqualTo(ContractStatus.DRAFT);
        assertThat(contract.authorId()).isEqualTo(author.response.userId());
        assertThat(contract.title()).isEqualTo(title);

        //ensuring of receicing message
        assertHasUnreadNotification(authorApi, manuscriptId, "Нова версія обкладинки");
        CoverVersionResponse approvedCover = approveCover(authorApi, manuscriptId, cover.versionNumber());
        assertThat(approvedCover.isApproved()).isTrue();

        //changing contract
        ContractResponse withRoyalty = updateRoyalty(accountantApi, contract.contractId(),
                new BigDecimal("17.50"), new BigDecimal("3000.00"));
        assertThat(withRoyalty.royaltyPercent()).isEqualByComparingTo("17.50");
        assertThat(withRoyalty.advancePayment()).isEqualByComparingTo("3000.00");

        awaitNotification(authorApi, manuscriptId, "Оновлення відсотка роялті");
        assertHasUnreadNotification(authorApi, manuscriptId, "Оновлення відсотка роялті");

        ContractResponse confirmed = confirmContract(authorApi, contract.contractId());
        assertThat(confirmed.authorConfirmedAt()).isNotNull();

        awaitNotification(authorApi, manuscriptId, "Контракт підтверджено");
        assertHasUnreadNotification(authorApi, manuscriptId, "Контракт підтверджено");

        //publishing
        ManuscriptResponse published = publish(chiefApi, manuscriptId);
        assertThat(published.status()).isEqualTo(ManuscriptStatus.PUBLISHED);

        awaitNotification(authorApi, manuscriptId, "Книгу опубліковано!");
        awaitContractStatus(authorApi, contract.contractId(), ContractStatus.ACTIVE);

        assertThat(myManuscripts(authorApi))
                .filteredOn(manuscript -> manuscriptId.equals(manuscript.manuscriptId()))
                .singleElement()
                .satisfies(manuscript -> assertThat(manuscript.status()).isEqualTo(ManuscriptStatus.PUBLISHED));

        assertThat(auditLog(chiefApi, manuscriptId))
                .extracting(ManuscriptAuditLogResponse::newStatus)
                .contains(ManuscriptStatus.TEXT_APPROVED, ManuscriptStatus.IN_DESIGN, ManuscriptStatus.PUBLISHED);

        List<NotificationResponse> unread = unreadNotifications(authorApi);
        assertThat(unread).isNotEmpty();
        NotificationResponse firstUnread = unread.getFirst();

        editorApi.patch().uri("/api/v1/notifications/{id}/read", firstUnread.id())
                .exchange()
                .expectStatus().isForbidden();

        NotificationResponse read = markAsRead(authorApi, firstUnread.id());
        assertThat(read.isRead()).isTrue();

    }

    private static class LoginResult {
        AuthResponse response;
        String refreshTokenCookie;

        LoginResult(AuthResponse response, String refreshTokenCookie) {
            this.response = response;
            this.refreshTokenCookie = refreshTokenCookie;
        }
    }

    private LoginResult loginWithCookie(String email) {
        var result = client.post().uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new LoginRequest(email, PASSWORD))
                .exchange()
                .expectStatus().isOk();

        AuthResponse response = result.expectBody(AuthResponse.class)
                .returnResult()
                .getResponseBody();

        String cookie = result.returnResult().getResponseHeaders().getFirst("Set-Cookie");
        String refreshTokenCookie = null;
        if (cookie != null && cookie.contains("refreshToken=")) {
            refreshTokenCookie = cookie.split("refreshToken=")[1].split(";")[0];
        }

        assertThat(response).isNotNull();
        assertThat(response.accessToken()).isNotBlank();
        assertThat(response.expiresIn()).isPositive();
        return new LoginResult(response, refreshTokenCookie);
    }

    private AuthResponse refresh(String refreshToken) {
        AuthResponse response = client.post().uri("/api/v1/auth/refresh")
                .cookie("refreshToken", refreshToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody(AuthResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(response).isNotNull();
        return response;
    }

    private RestTestClient authorized(AuthResponse auth) {
        return client.mutate()
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + auth.accessToken())
                .build();
    }

    private UUID createGenre(RestTestClient http, String name) {
        GenreResponse genre = http.post().uri("/api/v1/genres")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new GenreRequest(name))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(GenreResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(genre).isNotNull();
        assertThat(genre.genreName()).isEqualTo(name);
        return genre.genreId();
    }

    private ManuscriptResponse submitManuscript(RestTestClient http, String title, UUID genreId) {
        return http.post().uri("/api/v1/manuscripts")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ManuscriptSubmissionRequest(
                        title,
                        "End-to-end manuscript created by the REST test",
                        "https://storage.example.com/drafts/e2e-" + title + ".docx",
                        List.of(genreId)
                ))
                .exchange()
                .expectStatus().isOk()
                .expectBody(ManuscriptResponse.class)
                .returnResult()
                .getResponseBody();
    }

    private List<ManuscriptResponse> manuscriptsByStatus(RestTestClient http, ManuscriptStatus status) {
        return http.get()
                .uri(uri -> uri.path("/api/v1/manuscripts").queryParam("status", status).build())
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<List<ManuscriptResponse>>() {
                })
                .returnResult()
                .getResponseBody();
    }

    private List<ManuscriptResponse> myManuscripts(RestTestClient http) {
        return http.get().uri("/api/v1/manuscripts/my")
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<List<ManuscriptResponse>>() {
                })
                .returnResult()
                .getResponseBody();
    }

    private ManuscriptResponse approveManuscript(RestTestClient http, UUID manuscriptId, UUID editorId) {
        return http.post().uri("/api/v1/manuscripts/{id}/approve", manuscriptId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ManuscriptApprovalRequest(editorId))
                .exchange()
                .expectStatus().isOk()
                .expectBody(ManuscriptResponse.class)
                .returnResult()
                .getResponseBody();
    }

    private ChapterResponse createChapter(RestTestClient http, UUID manuscriptId, String title, int index) {
        return http.post().uri("/api/v1/manuscripts/{id}/chapters", manuscriptId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ChapterCreationRequest(title, index))
                .exchange()
                .expectStatus().isOk()
                .expectBody(ChapterResponse.class)
                .returnResult()
                .getResponseBody();
    }

    private ChapterResponse updateChapter(RestTestClient http, UUID chapterId, String title, int index) {
        return http.put().uri("/api/v1/chapters/{id}", chapterId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ChapterUpdateRequest(title, index))
                .exchange()
                .expectStatus().isOk()
                .expectBody(ChapterResponse.class)
                .returnResult()
                .getResponseBody();
    }

    private ManuscriptResponse finalizeText(RestTestClient http, UUID manuscriptId) {
        return http.post().uri("/api/v1/manuscripts/{id}/finalize-text", manuscriptId)
                .exchange()
                .expectStatus().isOk()
                .expectBody(ManuscriptResponse.class)
                .returnResult()
                .getResponseBody();
    }

    private ManuscriptResponse assignDesigner(RestTestClient http, UUID manuscriptId, UUID designerId) {
        return http.post().uri("/api/v1/manuscripts/{id}/assign-designer", manuscriptId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new AssignDesignerRequest(designerId))
                .exchange()
                .expectStatus().isOk()
                .expectBody(ManuscriptResponse.class)
                .returnResult()
                .getResponseBody();
    }

    private CoverVersionResponse uploadCover(RestTestClient http, UUID manuscriptId) {
        MultipartBodyBuilder parts = new MultipartBodyBuilder();
        parts.part("file", new ByteArrayResource(COVER_PNG))
                .filename("cover.png")
                .contentType(MediaType.IMAGE_PNG);

        return http.post().uri("/api/v1/manuscripts/{id}/cover-versions", manuscriptId)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(parts.build())
                .exchange()
                .expectStatus().isOk()
                .expectBody(CoverVersionResponse.class)
                .returnResult()
                .getResponseBody();
    }

    private CoverVersionResponse approveCover(RestTestClient http, UUID manuscriptId, int versionNumber) {
        return http.patch()
                .uri("/api/v1/manuscripts/{id}/cover-versions/{version}/approve", manuscriptId, versionNumber)
                .exchange()
                .expectStatus().isOk()
                .expectBody(CoverVersionResponse.class)
                .returnResult()
                .getResponseBody();
    }

    private ContractResponse awaitContract(RestTestClient http, UUID manuscriptId) {
        await().atMost(Duration.ofSeconds(15))
                .pollInterval(Duration.ofMillis(200))
                .untilAsserted(() -> assertThat(getContract(http, manuscriptId)).isNotNull());

        return getContract(http, manuscriptId);
    }

    private ContractResponse getContract(RestTestClient http, UUID manuscriptId) {
        return http.get()
                .uri(uri -> uri.path("/api/v1/contracts").queryParam("manuscriptId", manuscriptId).build())
                .exchange()
                .expectStatus().isOk()
                .expectBody(ContractResponse.class)
                .returnResult()
                .getResponseBody();
    }

    private void awaitContractStatus(RestTestClient http, UUID contractId, ContractStatus status) {
        await().atMost(Duration.ofSeconds(15))
                .pollInterval(Duration.ofMillis(200))
                .untilAsserted(() -> assertThat(getContractById(http, contractId).status()).isEqualTo(status));
    }

    private ContractResponse getContractById(RestTestClient http, UUID contractId) {
        return http.get().uri("/api/v1/contracts/{id}", contractId)
                .exchange()
                .expectStatus().isOk()
                .expectBody(ContractResponse.class)
                .returnResult()
                .getResponseBody();
    }

    private ContractResponse updateRoyalty(
            RestTestClient http,
            UUID contractId,
            BigDecimal royaltyPercent,
            BigDecimal advancePayment
    ) {
        return http.patch().uri("/api/v1/contracts/{id}/royalty", contractId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new RoyaltyUpdateRequest(
                        royaltyPercent,
                        advancePayment,
                        "E2E renegotiation of the royalty terms"
                ))
                .exchange()
                .expectStatus().isOk()
                .expectBody(ContractResponse.class)
                .returnResult()
                .getResponseBody();
    }

    private ContractResponse confirmContract(RestTestClient http, UUID contractId) {
        return http.post().uri("/api/v1/contracts/{id}/confirm", contractId)
                .exchange()
                .expectStatus().isOk()
                .expectBody(ContractResponse.class)
                .returnResult()
                .getResponseBody();
    }

    private ManuscriptResponse publish(RestTestClient http, UUID manuscriptId) {
        return http.post().uri("/api/v1/manuscripts/{id}/publish", manuscriptId)
                .exchange()
                .expectStatus().isOk()
                .expectBody(ManuscriptResponse.class)
                .returnResult()
                .getResponseBody();
    }

    private List<ManuscriptAuditLogResponse> auditLog(RestTestClient http, UUID manuscriptId) {
        return http.get().uri("/api/v1/manuscripts/{id}/audit-log", manuscriptId)
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<List<ManuscriptAuditLogResponse>>() {
                })
                .returnResult()
                .getResponseBody();
    }

    private List<NotificationResponse> notifications(RestTestClient http, boolean unreadOnly) {
        return http.get()
                .uri(uri -> uri.path("/api/v1/notifications")
                        .queryParam("unreadOnly", unreadOnly)
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<List<NotificationResponse>>() {
                })
                .returnResult()
                .getResponseBody();
    }

    private List<NotificationResponse> unreadNotifications(RestTestClient http) {
        return notifications(http, true);
    }

    private long unreadCount(RestTestClient http) {
        UnreadCountResponse response = http.get().uri("/api/v1/notifications/unread-count")
                .exchange()
                .expectStatus().isOk()
                .expectBody(UnreadCountResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(response).isNotNull();
        return response.unreadCount();
    }

    private NotificationResponse markAsRead(RestTestClient http, UUID notificationId) {
        return http.patch().uri("/api/v1/notifications/{id}/read", notificationId)
                .exchange()
                .expectStatus().isOk()
                .expectBody(NotificationResponse.class)
                .returnResult()
                .getResponseBody();
    }

    private void awaitNotification(RestTestClient http, UUID targetId, String title) {
        await().atMost(Duration.ofSeconds(15))
                .pollInterval(Duration.ofMillis(200))
                .untilAsserted(() -> assertThat(unreadNotifications(http))
                        .filteredOn(notification -> targetId.equals(notification.targetId())
                                && title.equals(notification.title()))
                        .isNotEmpty());
    }

    private void assertHasUnreadNotification(RestTestClient http, UUID targetId, String title) {
        assertThat(unreadNotifications(http))
                .filteredOn(notification -> targetId.equals(notification.targetId())
                        && title.equals(notification.title()))
                .isNotEmpty();
    }

    private void awaitUnreadCount(RestTestClient http, long expected) {
        await().atMost(Duration.ofSeconds(15))
                .pollInterval(Duration.ofMillis(200))
                .untilAsserted(() -> assertThat(unreadCount(http)).isEqualTo(expected));
    }
}
