package com.unibook.publisher.integration.support;

import com.unibook.publisher.common.enums.ContractStatus;
import com.unibook.publisher.common.seeding.SeedConstants;
import com.unibook.publisher.communication.entity.response.NotificationResponse;
import com.unibook.publisher.communication.entity.response.UnreadCountResponse;
import com.unibook.publisher.finance.entity.request.RoyaltyUpdateRequest;
import com.unibook.publisher.finance.entity.response.ContractResponse;
import com.unibook.publisher.identity.entity.request.LoginRequest;
import com.unibook.publisher.identity.entity.response.AuthResponse;
import jakarta.servlet.http.Cookie;
import com.unibook.publisher.production.entity.request.AssignDesignerRequest;
import com.unibook.publisher.production.entity.request.ChapterCreationRequest;
import com.unibook.publisher.production.entity.request.ChapterUpdateRequest;
import com.unibook.publisher.production.entity.request.GenreRequest;
import com.unibook.publisher.production.entity.request.ManuscriptApprovalRequest;
import com.unibook.publisher.production.entity.request.ManuscriptSubmissionRequest;
import com.unibook.publisher.production.entity.request.ManuscriptUpdateRequest;
import com.unibook.publisher.production.entity.response.ChapterResponse;
import com.unibook.publisher.production.entity.response.CoverVersionResponse;
import com.unibook.publisher.production.entity.response.GenreResponse;
import com.unibook.publisher.production.entity.response.ManuscriptAuditLogResponse;
import com.unibook.publisher.production.entity.response.ManuscriptResponse;
import com.unibook.publisher.production.enums.ManuscriptStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.AbstractMockHttpServletRequestBuilder;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

//helpers shared by integration tests
public abstract class ApiIntegrationTestSupport {

    protected static final String PASSWORD = SeedConstants.DEFAULT_PASSWORD;

    protected static final String AUTHOR_EMAIL = "author1@unibook-demo.local";
    protected static final String AUTHOR_2_EMAIL = "author2@unibook-demo.local";
    protected static final String CHIEF_EDITOR_EMAIL = "chief.editor@unibook-demo.local";
    protected static final String EDITOR_EMAIL = "editor@unibook-demo.local";
    protected static final String DESIGNER_EMAIL = "designer@unibook-demo.local";
    protected static final String ACCOUNTANT_EMAIL = "accountant@unibook-demo.local";
    protected static final String ADMIN_EMAIL = "admin@unibook-demo.local";

    protected static final UUID AUTHOR_ID = SeedConstants.AUTHOR_1_ID;
    protected static final UUID AUTHOR_2_ID = SeedConstants.AUTHOR_2_ID;
    protected static final UUID CHIEF_EDITOR_ID = SeedConstants.CHIEF_EDITOR_ID;
    protected static final UUID EDITOR_ID = SeedConstants.EDITOR_ID;
    protected static final UUID DESIGNER_ID = SeedConstants.DESIGNER_ID;
    protected static final UUID ACCOUNTANT_ID = SeedConstants.ACCOUNTANT_ID;
    protected static final UUID ADMIN_ID = SeedConstants.ADMIN_ID;

    private static final byte[] COVER_PNG = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg=="
    );

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected JdbcClient jdbc;

    protected AuthResponse login(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(email, PASSWORD))))
                .andExpect(status().isOk())
                .andReturn();

        AuthResponse response = read(result, AuthResponse.class);
        assertThat(response.accessToken()).isNotBlank();
        assertThat(response.expiresIn()).isPositive();
        return response;
    }

    protected AuthResponse refresh(String refreshToken) throws Exception {
        Cookie cookie = new Cookie("refreshToken", refreshToken);
        MvcResult result = mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(cookie))
                .andExpect(status().isOk())
                .andReturn();

        return read(result, AuthResponse.class);
    }

    protected AuthResponse refreshWithCookie(MvcResult loginResult) throws Exception {
        Cookie cookie = loginResult.getResponse().getCookie("refreshToken");
        assertThat(cookie).isNotNull();
        MvcResult result = mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(cookie))
                .andExpect(status().isOk())
                .andReturn();

        return read(result, AuthResponse.class);
    }

    protected Cookie getRefreshCookie(MvcResult result) {
        return result.getResponse().getCookie("refreshToken");
    }

    protected <T extends AbstractMockHttpServletRequestBuilder<T>> T authorized(T request, AuthResponse auth) {
        request.header(HttpHeaders.AUTHORIZATION, "Bearer " + auth.accessToken());
        return request;
    }

    protected GenreResponse createGenre(AuthResponse auth, String name) throws Exception {
        MvcResult result = mockMvc.perform(authorized(post("/api/v1/genres"), auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new GenreRequest(name))))
                .andExpect(status().isCreated())
                .andReturn();

        return read(result, GenreResponse.class);
    }

    protected ManuscriptResponse submitManuscript(AuthResponse auth, String title, UUID genreId) throws Exception {
        return submitManuscript(
                auth,
                title,
                "Manuscript created by a MockMvc integration test",
                "https://storage.example.com/drafts/" + UUID.randomUUID() + ".docx",
                List.of(genreId)
        );
    }

    protected ManuscriptResponse submitManuscript(
            AuthResponse auth,
            String title,
            String annotation,
            String draftFileUrl,
            List<UUID> genreIds
    ) throws Exception {
        MvcResult result = mockMvc.perform(authorized(post("/api/v1/manuscripts"), auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ManuscriptSubmissionRequest(
                                title,
                                annotation,
                                draftFileUrl,
                                genreIds
                        ))))
                .andExpect(status().isOk())
                .andReturn();

        return read(result, ManuscriptResponse.class);
    }

    protected ManuscriptResponse getManuscript(AuthResponse auth, UUID manuscriptId) throws Exception {
        MvcResult result = mockMvc.perform(authorized(
                        get("/api/v1/manuscripts/{id}", manuscriptId), auth))
                .andExpect(status().isOk())
                .andReturn();

        return read(result, ManuscriptResponse.class);
    }

    protected ManuscriptResponse updateManuscript(
            AuthResponse auth,
            UUID manuscriptId,
            String title,
            String annotation,
            String draftFileUrl,
            Set<UUID> genreIds
    ) throws Exception {
        MvcResult result = mockMvc.perform(authorized(
                        put("/api/v1/manuscripts/{id}", manuscriptId), auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ManuscriptUpdateRequest(
                                title,
                                annotation,
                                draftFileUrl,
                                genreIds
                        ))))
                .andExpect(status().isOk())
                .andReturn();

        return read(result, ManuscriptResponse.class);
    }

    protected void deleteManuscript(AuthResponse auth, UUID manuscriptId) throws Exception {
        mockMvc.perform(authorized(delete("/api/v1/manuscripts/{id}", manuscriptId), auth))
                .andExpect(status().isNoContent());
    }

    protected List<ManuscriptResponse> manuscriptsByStatus(AuthResponse auth, ManuscriptStatus status)
            throws Exception {
        MvcResult result = mockMvc.perform(authorized(
                        get("/api/v1/manuscripts").param("status", status.name()), auth))
                .andExpect(status().isOk())
                .andReturn();

        return readList(result, new TypeReference<>() {
        });
    }

    protected List<ManuscriptResponse> myManuscripts(AuthResponse auth) throws Exception {
        MvcResult result = mockMvc.perform(authorized(get("/api/v1/manuscripts/my"), auth))
                .andExpect(status().isOk())
                .andReturn();

        return readList(result, new TypeReference<>() {
        });
    }

    protected ManuscriptResponse approveManuscript(AuthResponse auth, UUID manuscriptId, UUID editorId)
            throws Exception {
        MvcResult result = mockMvc.perform(authorized(
                        post("/api/v1/manuscripts/{id}/approve", manuscriptId), auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ManuscriptApprovalRequest(editorId))))
                .andExpect(status().isOk())
                .andReturn();

        return read(result, ManuscriptResponse.class);
    }

    protected ManuscriptResponse submitAndApprove(
            AuthResponse author,
            AuthResponse chiefEditor,
            UUID genreId,
            String title
    ) throws Exception {
        ManuscriptResponse submitted = submitManuscript(author, title, genreId);
        return approveManuscript(chiefEditor, submitted.manuscriptId(), EDITOR_ID);
    }

    protected ChapterResponse createChapter(
            AuthResponse auth,
            UUID manuscriptId,
            String title,
            int index
    ) throws Exception {
        MvcResult result = mockMvc.perform(authorized(
                        post("/api/v1/manuscripts/{id}/chapters", manuscriptId), auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ChapterCreationRequest(title, index))))
                .andExpect(status().isOk())
                .andReturn();

        return read(result, ChapterResponse.class);
    }

    protected List<ChapterResponse> chapters(AuthResponse auth, UUID manuscriptId) throws Exception {
        MvcResult result = mockMvc.perform(authorized(
                        get("/api/v1/manuscripts/{id}/chapters", manuscriptId), auth))
                .andExpect(status().isOk())
                .andReturn();

        return readList(result, new TypeReference<>() {
        });
    }

    protected ChapterResponse updateChapter(
            AuthResponse auth,
            UUID chapterId,
            String title,
            int index
    ) throws Exception {
        MvcResult result = mockMvc.perform(authorized(put("/api/v1/chapters/{id}", chapterId), auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ChapterUpdateRequest(title, index))))
                .andExpect(status().isOk())
                .andReturn();

        return read(result, ChapterResponse.class);
    }

    protected void deleteChapter(AuthResponse auth, UUID chapterId) throws Exception {
        mockMvc.perform(authorized(delete("/api/v1/chapters/{id}", chapterId), auth))
                .andExpect(status().isNoContent());
    }

    protected ManuscriptResponse finalizeText(AuthResponse auth, UUID manuscriptId) throws Exception {
        MvcResult result = mockMvc.perform(authorized(
                        post("/api/v1/manuscripts/{id}/finalize-text", manuscriptId), auth))
                .andExpect(status().isOk())
                .andReturn();

        return read(result, ManuscriptResponse.class);
    }

    protected ManuscriptResponse assignDesigner(AuthResponse auth, UUID manuscriptId, UUID designerId)
            throws Exception {
        MvcResult result = mockMvc.perform(authorized(
                        post("/api/v1/manuscripts/{id}/assign-designer", manuscriptId), auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AssignDesignerRequest(designerId))))
                .andExpect(status().isOk())
                .andReturn();

        return read(result, ManuscriptResponse.class);
    }

    protected ManuscriptResponse prepareManuscriptInDesign(
            AuthResponse author,
            AuthResponse chiefEditor,
            AuthResponse editor,
            UUID genreId,
            String title
    ) throws Exception {
        ManuscriptResponse inProgress = submitAndApprove(author, chiefEditor, genreId, title);
        finalizeText(editor, inProgress.manuscriptId());
        return assignDesigner(chiefEditor, inProgress.manuscriptId(), DESIGNER_ID);
    }

    protected CoverVersionResponse uploadCover(AuthResponse auth, UUID manuscriptId) throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "cover.png",
                MediaType.IMAGE_PNG_VALUE,
                COVER_PNG
        );

        MvcResult result = mockMvc.perform(authorized(
                        multipart("/api/v1/manuscripts/{id}/cover-versions", manuscriptId).file(file), auth))
                .andExpect(status().isOk())
                .andReturn();

        return read(result, CoverVersionResponse.class);
    }

    protected List<CoverVersionResponse> coverVersions(AuthResponse auth, UUID manuscriptId) throws Exception {
        MvcResult result = mockMvc.perform(authorized(
                        get("/api/v1/manuscripts/{id}/cover-versions", manuscriptId), auth))
                .andExpect(status().isOk())
                .andReturn();

        return readList(result, new TypeReference<>() {
        });
    }

    protected CoverVersionResponse approveCover(AuthResponse auth, UUID manuscriptId, int versionNumber)
            throws Exception {
        MvcResult result = mockMvc.perform(authorized(
                        patch("/api/v1/manuscripts/{id}/cover-versions/{version}/approve",
                                manuscriptId, versionNumber), auth))
                .andExpect(status().isOk())
                .andReturn();

        return read(result, CoverVersionResponse.class);
    }

    protected ContractResponse getContractByManuscript(AuthResponse auth, UUID manuscriptId) throws Exception {
        MvcResult result = mockMvc.perform(authorized(
                        get("/api/v1/contracts").param("manuscriptId", manuscriptId.toString()), auth))
                .andExpect(status().isOk())
                .andReturn();

        return read(result, ContractResponse.class);
    }

    protected ContractResponse getContractById(AuthResponse auth, UUID contractId) throws Exception {
        MvcResult result = mockMvc.perform(authorized(get("/api/v1/contracts/{id}", contractId), auth))
                .andExpect(status().isOk())
                .andReturn();

        return read(result, ContractResponse.class);
    }

    protected ContractResponse updateRoyalty(
            AuthResponse auth,
            UUID contractId,
            BigDecimal royaltyPercent,
            BigDecimal advancePayment
    ) throws Exception {
        return updateRoyalty(auth, contractId, royaltyPercent, advancePayment,
                "Integration test renegotiation of royalty terms");
    }

    protected ContractResponse updateRoyalty(
            AuthResponse auth,
            UUID contractId,
            BigDecimal royaltyPercent,
            BigDecimal advancePayment,
            String reason
    ) throws Exception {
        MvcResult result = mockMvc.perform(authorized(
                        patch("/api/v1/contracts/{id}/royalty", contractId), auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RoyaltyUpdateRequest(
                                royaltyPercent,
                                advancePayment,
                                reason
                        ))))
                .andExpect(status().isOk())
                .andReturn();

        return read(result, ContractResponse.class);
    }

    protected ContractResponse confirmContract(AuthResponse auth, UUID contractId) throws Exception {
        MvcResult result = mockMvc.perform(authorized(
                        post("/api/v1/contracts/{id}/confirm", contractId), auth))
                .andExpect(status().isOk())
                .andReturn();

        return read(result, ContractResponse.class);
    }

    protected ManuscriptResponse publish(AuthResponse auth, UUID manuscriptId) throws Exception {
        MvcResult result = mockMvc.perform(authorized(
                        post("/api/v1/manuscripts/{id}/publish", manuscriptId), auth))
                .andExpect(status().isOk())
                .andReturn();

        return read(result, ManuscriptResponse.class);
    }

    protected List<ManuscriptAuditLogResponse> auditLog(AuthResponse auth, UUID manuscriptId) throws Exception {
        MvcResult result = mockMvc.perform(authorized(
                        get("/api/v1/manuscripts/{id}/audit-log", manuscriptId), auth))
                .andExpect(status().isOk())
                .andReturn();

        return readList(result, new TypeReference<>() {
        });
    }

    protected List<NotificationResponse> notifications(AuthResponse auth, boolean unreadOnly) throws Exception {
        MvcResult result = mockMvc.perform(authorized(
                        get("/api/v1/notifications").param("unreadOnly", Boolean.toString(unreadOnly)), auth))
                .andExpect(status().isOk())
                .andReturn();

        return readList(result, new TypeReference<>() {
        });
    }

    protected List<NotificationResponse> unreadNotifications(AuthResponse auth) throws Exception {
        return notifications(auth, true);
    }

    protected long unreadCount(AuthResponse auth) throws Exception {
        MvcResult result = mockMvc.perform(authorized(
                        get("/api/v1/notifications/unread-count"), auth))
                .andExpect(status().isOk())
                .andReturn();

        UnreadCountResponse response = read(result, UnreadCountResponse.class);
        return response.unreadCount();
    }

    protected NotificationResponse markAsRead(AuthResponse auth, UUID notificationId) throws Exception {
        MvcResult result = mockMvc.perform(authorized(
                        patch("/api/v1/notifications/{id}/read", notificationId), auth))
                .andExpect(status().isOk())
                .andReturn();

        return read(result, NotificationResponse.class);
    }

    protected void markAllAsRead(AuthResponse auth) throws Exception {
        mockMvc.perform(authorized(patch("/api/v1/notifications/read-all"), auth))
                .andExpect(status().isNoContent());
    }

    protected ContractResponse awaitContract(AuthResponse auth, UUID manuscriptId) throws Exception {
        await().atMost(Duration.ofSeconds(20))
                .pollInterval(Duration.ofMillis(200))
                .untilAsserted(() -> assertThat(dbCount(
                        "select count(*) from contracts where manuscript_id = ?", manuscriptId))
                        .isEqualTo(1));

        return getContractByManuscript(auth, manuscriptId);
    }

    protected void awaitContractStatus(UUID contractId, ContractStatus status) {
        await().atMost(Duration.ofSeconds(20))
                .pollInterval(Duration.ofMillis(200))
                .untilAsserted(() -> assertThat(dbString(
                        "select status from contracts where contract_id = ?", contractId))
                        .isEqualTo(status.name()));
    }

    protected NotificationResponse awaitNotification(AuthResponse auth, UUID targetId, String title)
            throws Exception {
        await().atMost(Duration.ofSeconds(20))
                .pollInterval(Duration.ofMillis(200))
                .untilAsserted(() -> assertThat(dbCount(
                        "select count(*) from notifications "
                                + "where recipient_id = ? and target_id = ? and title = ?",
                        auth.userId(), targetId, title))
                        .isEqualTo(1));

        return unreadNotifications(auth).stream()
                .filter(notification -> targetId.equals(notification.targetId())
                        && title.equals(notification.title()))
                .findFirst()
                .orElseThrow();
    }

    protected long dbCount(String sql, Object... args) {
        return jdbc.sql(sql).params(args).query(Long.class).single();
    }

    protected String dbString(String sql, Object... args) {
        return jdbc.sql(sql).params(args).query(String.class).single();
    }

    protected UUID dbUuid(String sql, Object... args) {
        return jdbc.sql(sql).params(args).query(UUID.class).single();
    }

    protected BigDecimal dbDecimal(String sql, Object... args) {
        return jdbc.sql(sql).params(args).query(BigDecimal.class).single();
    }

    protected boolean dbBoolean(String sql, Object... args) {
        return jdbc.sql(sql).params(args).query(Boolean.class).single();
    }

    protected <T> T read(MvcResult result, Class<T> type) throws Exception {
        return objectMapper.readValue(result.getResponse().getContentAsString(StandardCharsets.UTF_8), type);
    }

    protected <T> List<T> readList(MvcResult result, TypeReference<List<T>> type) throws Exception {
        return objectMapper.readValue(result.getResponse().getContentAsString(StandardCharsets.UTF_8), type);
    }
}
