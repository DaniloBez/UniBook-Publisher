package com.unibook.publisher.integration;

import com.unibook.publisher.identity.entity.response.AuthResponse;
import com.unibook.publisher.integration.support.ApiIntegrationTestSupport;
import com.unibook.publisher.production.entity.response.ChapterResponse;
import com.unibook.publisher.production.entity.response.ManuscriptResponse;
import com.unibook.publisher.support.IntegrationTest;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
class ChapterApiIntegrationTest extends ApiIntegrationTestSupport {

    @Test
    void chapterCrud_ShouldPersistReadUpdateAndDeleteRow() throws Exception {
        AuthResponse author = login(AUTHOR_EMAIL);
        AuthResponse chiefEditor = login(CHIEF_EDITOR_EMAIL);
        UUID genreId = createGenre(chiefEditor, "IT Genre " + UUID.randomUUID()).genreId();
        ManuscriptResponse manuscript = submitAndApprove(
                author, chiefEditor, genreId, "IT Chapter Manuscript " + UUID.randomUUID());

        ChapterResponse created = createChapter(
                author, manuscript.manuscriptId(), "IT Chapter One", 1);

        assertThat(created.manuscriptId()).isEqualTo(manuscript.manuscriptId());
        assertThat(dbString("select chapter_title from chapters where chapter_id = ?", created.chapterId()))
                .isEqualTo("IT Chapter One");
        assertThat(dbCount(
                "select count(*) from chapters where chapter_id = ? and chapter_index = 1",
                created.chapterId()))
                .isEqualTo(1);
        assertThat(dbUuid("select manuscript_id from chapters where chapter_id = ?", created.chapterId()))
                .isEqualTo(manuscript.manuscriptId());

        assertThat(chapters(author, manuscript.manuscriptId()))
                .extracting(ChapterResponse::chapterId)
                .contains(created.chapterId());

        ChapterResponse updated = updateChapter(
                author, created.chapterId(), "IT Chapter One Revised", 2);

        assertThat(updated.chapterTitle()).isEqualTo("IT Chapter One Revised");
        assertThat(updated.chapterIndex()).isEqualTo(2);
        assertThat(dbString(
                "select chapter_title from chapters where chapter_id = ?", created.chapterId()))
                .isEqualTo("IT Chapter One Revised");
        assertThat(dbCount(
                "select count(*) from chapters where chapter_id = ? and chapter_index = 2",
                created.chapterId()))
                .isEqualTo(1);

        deleteChapter(author, created.chapterId());

        assertThat(dbCount("select count(*) from chapters where chapter_id = ?", created.chapterId()))
                .isZero();
    }
}
