package com.unibook.publisher.integration;

import com.unibook.publisher.common.enums.UserRole;
import com.unibook.publisher.identity.entity.request.LoginRequest;
import com.unibook.publisher.identity.entity.response.AuthResponse;
import com.unibook.publisher.integration.support.ApiIntegrationTestSupport;
import com.unibook.publisher.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class AuthApiIntegrationTest extends ApiIntegrationTestSupport {

    @Test
    void login_ShouldReturnTokensAndSeededAuthorProfile() throws Exception {
        AuthResponse response = login(AUTHOR_EMAIL);

        assertThat(response.userId()).isEqualTo(AUTHOR_ID);
        assertThat(response.role()).isEqualTo(UserRole.AUTHOR);
        assertThat(response.accessToken()).isNotBlank();
        assertThat(response.expiresIn()).isPositive();

        assertThat(dbString("select email from users where id = ?", AUTHOR_ID))
                .isEqualTo(AUTHOR_EMAIL);
        assertThat(dbCount("select count(*) from user_profiles where user_id = ?", AUTHOR_ID))
                .isEqualTo(1);
    }

    @Test
    void refresh_ShouldProduceAccessTokenAcceptedByProtectedEndpoint() throws Exception {
        org.springframework.test.web.servlet.MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new com.unibook.publisher.identity.entity.request.LoginRequest(AUTHOR_EMAIL, PASSWORD))))
                .andExpect(status().isOk())
                .andReturn();

        AuthResponse original = read(loginResult, AuthResponse.class);

        AuthResponse refreshed = refreshWithCookie(loginResult);

        assertThat(refreshed.userId()).isEqualTo(AUTHOR_ID);
        assertThat(refreshed.accessToken()).isNotBlank();

        mockMvc.perform(authorized(get("/api/v1/manuscripts/my"), refreshed))
                .andExpect(status().isOk());
    }

    @Test
    void logout_ShouldReturnNoContent() throws Exception {
        AuthResponse author = login(AUTHOR_EMAIL);

        mockMvc.perform(authorized(post("/api/v1/auth/logout"), author))
                .andExpect(status().isNoContent());
    }

    @Test
    void login_WithWrongPassword_ShouldReturnUnauthorizedProblemDetail() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest(AUTHOR_EMAIL, "wrong-password"))))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void protectedApi_WithoutToken_ShouldReturnUnauthorizedProblemDetail() throws Exception {
        mockMvc.perform(get("/api/v1/genres"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(401));
    }
}
