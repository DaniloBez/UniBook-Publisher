package com.unibook.publisher.integration;

import com.unibook.publisher.identity.entity.response.AuthResponse;
import com.unibook.publisher.integration.support.ApiIntegrationTestSupport;
import com.unibook.publisher.production.entity.response.GenreResponse;
import com.unibook.publisher.production.entity.response.ManuscriptResponse;
import com.unibook.publisher.production.enums.ManuscriptStatus;
import com.unibook.publisher.support.IntegrationTest;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class ManuscriptApiIntegrationTest extends ApiIntegrationTestSupport {

    @Test
    void create_ShouldPersistManuscriptRowAndGenreLink() throws Exception {
        AuthResponse author = login(AUTHOR_EMAIL);
        AuthResponse chiefEditor = login(CHIEF_EDITOR_EMAIL);
        UUID genreId = createGenre(chiefEditor, "IT Genre " + UUID.randomUUID()).genreId();
        String title = "IT Manuscript " + UUID.randomUUID();

        ManuscriptResponse created = submitManuscript(author, title, genreId);
        UUID manuscriptId = created.manuscriptId();

        assertThat(created.title()).isEqualTo(title);
        assertThat(created.authorId()).isEqualTo(AUTHOR_ID);
        assertThat(created.status()).isEqualTo(ManuscriptStatus.SUBMITTED);
        assertThat(created.genres()).extracting(GenreResponse::genreId).containsExactly(genreId);

        assertThat(dbString("select title from manuscripts where manuscript_id = ?", manuscriptId))
                .isEqualTo(title);
        assertThat(dbUuid("select author_id from manuscripts where manuscript_id = ?", manuscriptId))
                .isEqualTo(AUTHOR_ID);
        assertThat(dbString("select status from manuscripts where manuscript_id = ?", manuscriptId))
                .isEqualTo(ManuscriptStatus.SUBMITTED.name());
        assertThat(dbCount(
                "select count(*) from manuscripts where manuscript_id = ? and submitted_at is not null",
                manuscriptId))
                .isEqualTo(1);
        assertThat(dbCount(
                "select count(*) from manuscript_genres where manuscript_id = ? and genre_id = ?",
                manuscriptId, genreId))
                .isEqualTo(1);
    }

    @Test
    void read_ShouldReturnManuscriptInDetailStatusAndMyLists() throws Exception {
        AuthResponse author = login(AUTHOR_EMAIL);
        AuthResponse chiefEditor = login(CHIEF_EDITOR_EMAIL);
        UUID genreId = createGenre(chiefEditor, "IT Genre " + UUID.randomUUID()).genreId();
        String title = "IT Manuscript " + UUID.randomUUID();
        UUID manuscriptId = submitManuscript(author, title, genreId).manuscriptId();

        mockMvc.perform(authorized(get("/api/v1/manuscripts/{id}", manuscriptId), chiefEditor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.manuscript_id").value(manuscriptId.toString()))
                .andExpect(jsonPath("$.title").value(title))
                .andExpect(jsonPath("$.status").value(ManuscriptStatus.SUBMITTED.name()));

        assertThat(manuscriptsByStatus(chiefEditor, ManuscriptStatus.SUBMITTED))
                .extracting(ManuscriptResponse::manuscriptId)
                .contains(manuscriptId);
        assertThat(myManuscripts(author))
                .extracting(ManuscriptResponse::manuscriptId)
                .contains(manuscriptId);
    }

    @Test
    void update_ShouldReplaceEditableFieldsAndGenreLinks() throws Exception {
        AuthResponse author = login(AUTHOR_EMAIL);
        AuthResponse chiefEditor = login(CHIEF_EDITOR_EMAIL);
        UUID firstGenreId = createGenre(chiefEditor, "IT Genre " + UUID.randomUUID()).genreId();
        UUID secondGenreId = createGenre(chiefEditor, "IT Genre " + UUID.randomUUID()).genreId();
        UUID manuscriptId = submitManuscript(
                author, "IT Manuscript " + UUID.randomUUID(), firstGenreId).manuscriptId();

        String updatedTitle = "IT Manuscript Updated " + UUID.randomUUID();
        String updatedAnnotation = "Updated annotation from the integration test";
        String updatedDraftUrl = "https://storage.example.com/drafts/updated-" + UUID.randomUUID() + ".docx";

        ManuscriptResponse updated = updateManuscript(
                author,
                manuscriptId,
                updatedTitle,
                updatedAnnotation,
                updatedDraftUrl,
                Set.of(firstGenreId, secondGenreId)
        );

        assertThat(updated.title()).isEqualTo(updatedTitle);
        assertThat(updated.annotation()).isEqualTo(updatedAnnotation);
        assertThat(updated.draftFileUrl()).isEqualTo(updatedDraftUrl);
        assertThat(updated.genres()).extracting(GenreResponse::genreId)
                .containsExactlyInAnyOrder(firstGenreId, secondGenreId);

        assertThat(dbString("select title from manuscripts where manuscript_id = ?", manuscriptId))
                .isEqualTo(updatedTitle);
        assertThat(dbString("select annotation from manuscripts where manuscript_id = ?", manuscriptId))
                .isEqualTo(updatedAnnotation);
        assertThat(dbString("select draft_file_url from manuscripts where manuscript_id = ?", manuscriptId))
                .isEqualTo(updatedDraftUrl);
        assertThat(dbCount("select count(*) from manuscript_genres where manuscript_id = ?", manuscriptId))
                .isEqualTo(2);
        assertThat(dbCount(
                "select count(*) from manuscript_genres where manuscript_id = ? and genre_id = ?",
                manuscriptId, secondGenreId))
                .isEqualTo(1);
    }

    @Test
    void delete_ShouldRemoveManuscriptAndGenreLinks() throws Exception {
        AuthResponse author = login(AUTHOR_EMAIL);
        AuthResponse chiefEditor = login(CHIEF_EDITOR_EMAIL);
        UUID genreId = createGenre(chiefEditor, "IT Genre " + UUID.randomUUID()).genreId();
        UUID manuscriptId = submitManuscript(
                author, "IT Manuscript " + UUID.randomUUID(), genreId).manuscriptId();

        deleteManuscript(author, manuscriptId);

        assertThat(dbCount("select count(*) from manuscripts where manuscript_id = ?", manuscriptId))
                .isZero();
        assertThat(dbCount("select count(*) from manuscript_genres where manuscript_id = ?", manuscriptId))
                .isZero();
    }
}
