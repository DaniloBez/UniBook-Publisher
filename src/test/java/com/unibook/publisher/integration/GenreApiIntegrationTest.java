package com.unibook.publisher.integration;

import com.unibook.publisher.identity.entity.response.AuthResponse;
import com.unibook.publisher.integration.support.ApiIntegrationTestSupport;
import com.unibook.publisher.production.entity.request.GenreRequest;
import com.unibook.publisher.production.entity.response.GenreResponse;
import com.unibook.publisher.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import tools.jackson.core.type.TypeReference;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class GenreApiIntegrationTest extends ApiIntegrationTestSupport {

    @Test
    void crud_ShouldPersistReadUpdateAndDeleteGenre() throws Exception {
        AuthResponse chiefEditor = login(CHIEF_EDITOR_EMAIL);
        String genreName = "IT Genre " + UUID.randomUUID();

        GenreResponse created = createGenre(chiefEditor, genreName);

        assertThat(dbString("select genre_name from genres where genre_id = ?", created.genreId()))
                .isEqualTo(genreName);

        mockMvc.perform(authorized(get("/api/v1/genres/{id}", created.genreId()), chiefEditor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.genre_id").value(created.genreId().toString()))
                .andExpect(jsonPath("$.genreName").value(genreName));

        mockMvc.perform(authorized(
                        get("/api/v1/genres/search").param("name", genreName), chiefEditor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.genre_id").value(created.genreId().toString()));

        List<GenreResponse> genres = readList(
                mockMvc.perform(authorized(get("/api/v1/genres"), chiefEditor))
                        .andExpect(status().isOk())
                        .andReturn(),
                new TypeReference<>() {
                }
        );
        assertThat(genres).extracting(GenreResponse::genreId).contains(created.genreId());

        String updatedName = "IT Genre Updated " + UUID.randomUUID();
        mockMvc.perform(authorized(put("/api/v1/genres/{id}", created.genreId()), chiefEditor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new GenreRequest(updatedName))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.genreName").value(updatedName));

        assertThat(dbString("select genre_name from genres where genre_id = ?", created.genreId()))
                .isEqualTo(updatedName);

        mockMvc.perform(authorized(delete("/api/v1/genres/{id}", created.genreId()), chiefEditor))
                .andExpect(status().isNoContent());

        assertThat(dbCount("select count(*) from genres where genre_id = ?", created.genreId()))
                .isZero();
    }

    @Test
    void createDuplicate_ShouldReturnConflictAndKeepSingleRow() throws Exception {
        AuthResponse chiefEditor = login(CHIEF_EDITOR_EMAIL);
        String genreName = "IT Duplicate Genre " + UUID.randomUUID();
        createGenre(chiefEditor, genreName);

        mockMvc.perform(authorized(post("/api/v1/genres"), chiefEditor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new GenreRequest(genreName))))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));

        assertThat(dbCount("select count(*) from genres where genre_name = ?", genreName))
                .isEqualTo(1);
    }
}
