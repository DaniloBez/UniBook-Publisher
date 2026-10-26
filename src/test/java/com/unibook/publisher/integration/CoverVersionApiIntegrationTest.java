package com.unibook.publisher.integration;

import com.unibook.publisher.identity.entity.response.AuthResponse;
import com.unibook.publisher.integration.support.ApiIntegrationTestSupport;
import com.unibook.publisher.production.entity.response.CoverVersionResponse;
import com.unibook.publisher.production.entity.response.ManuscriptResponse;
import com.unibook.publisher.production.enums.ManuscriptStatus;
import com.unibook.publisher.support.IntegrationTest;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
class CoverVersionApiIntegrationTest extends ApiIntegrationTestSupport {

    @Test
    void covers_ShouldPersistVersionsAndApproveOnlySelectedOne() throws Exception {
        AuthResponse author = login(AUTHOR_EMAIL);
        AuthResponse chiefEditor = login(CHIEF_EDITOR_EMAIL);
        AuthResponse editor = login(EDITOR_EMAIL);
        AuthResponse designer = login(DESIGNER_EMAIL);
        UUID genreId = createGenre(chiefEditor, "IT Genre " + UUID.randomUUID()).genreId();

        ManuscriptResponse inDesign = prepareManuscriptInDesign(
                author,
                chiefEditor,
                editor,
                genreId,
                "IT Cover Manuscript " + UUID.randomUUID()
        );
        UUID manuscriptId = inDesign.manuscriptId();

        assertThat(inDesign.status()).isEqualTo(ManuscriptStatus.IN_DESIGN);
        assertThat(dbString("select status from manuscripts where manuscript_id = ?", manuscriptId))
                .isEqualTo(ManuscriptStatus.IN_DESIGN.name());

        CoverVersionResponse first = uploadCover(designer, manuscriptId);

        assertThat(first.versionNumber()).isEqualTo(1);
        assertThat(first.isApproved()).isFalse();
        assertThat(first.uploadedByUserId()).isEqualTo(DESIGNER_ID);
        assertThat(first.fileUrl()).isNotBlank();
        assertThat(dbCount(
                "select count(*) from cover_versions where id = ? and manuscript_id = ? "
                        + "and uploaded_by_user_id = ? and version_number = 1 and is_approved = false",
                first.id(), manuscriptId, DESIGNER_ID))
                .isEqualTo(1);

        assertThat(coverVersions(author, manuscriptId))
                .extracting(CoverVersionResponse::id)
                .contains(first.id());

        CoverVersionResponse approvedFirst = approveCover(author, manuscriptId, 1);

        assertThat(approvedFirst.isApproved()).isTrue();
        assertThat(dbBoolean(
                "select is_approved from cover_versions where id = ?", first.id()))
                .isTrue();

        CoverVersionResponse second = uploadCover(designer, manuscriptId);
        assertThat(second.versionNumber()).isEqualTo(2);

        CoverVersionResponse approvedSecond = approveCover(author, manuscriptId, 2);

        assertThat(approvedSecond.isApproved()).isTrue();
        assertThat(dbBoolean(
                "select is_approved from cover_versions where id = ?", second.id()))
                .isTrue();
        assertThat(dbBoolean(
                "select is_approved from cover_versions where id = ?", first.id()))
                .isFalse();
        assertThat(dbCount(
                "select count(*) from cover_versions where manuscript_id = ? and is_approved = true",
                manuscriptId))
                .isEqualTo(1);
    }
}
