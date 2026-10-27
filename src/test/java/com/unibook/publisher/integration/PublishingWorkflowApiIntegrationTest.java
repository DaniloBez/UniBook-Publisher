package com.unibook.publisher.integration;

import com.unibook.publisher.common.enums.ContractStatus;
import com.unibook.publisher.finance.entity.response.ContractResponse;
import com.unibook.publisher.identity.entity.response.AuthResponse;
import com.unibook.publisher.integration.support.ApiIntegrationTestSupport;
import com.unibook.publisher.production.entity.response.ChapterResponse;
import com.unibook.publisher.production.entity.response.CoverVersionResponse;
import com.unibook.publisher.production.entity.response.ManuscriptAuditLogResponse;
import com.unibook.publisher.production.entity.response.ManuscriptResponse;
import com.unibook.publisher.production.enums.ManuscriptStatus;
import com.unibook.publisher.support.IntegrationTest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
class PublishingWorkflowApiIntegrationTest extends ApiIntegrationTestSupport {

    @Test
    void manuscript_ShouldTravelFromSubmissionToPublicationAndPersistEveryStage() throws Exception {
        AuthResponse chiefEditor = login(CHIEF_EDITOR_EMAIL);
        AuthResponse editor = login(EDITOR_EMAIL);
        AuthResponse designer = login(DESIGNER_EMAIL);
        AuthResponse author = login(AUTHOR_EMAIL);
        AuthResponse accountant = login(ACCOUNTANT_EMAIL);

        assertThat(dbString("select role from users where id = ?", CHIEF_EDITOR_ID))
                .isEqualTo("CHIEF_EDITOR");
        assertThat(dbString("select role from users where id = ?", EDITOR_ID))
                .isEqualTo("EDITOR");
        assertThat(dbString("select role from users where id = ?", DESIGNER_ID))
                .isEqualTo("DESIGNER");
        assertThat(dbString("select role from users where id = ?", AUTHOR_ID))
                .isEqualTo("AUTHOR");
        assertThat(dbString("select role from users where id = ?", ACCOUNTANT_ID))
                .isEqualTo("ACCOUNTANT");

        String suffix = UUID.randomUUID().toString();
        UUID genreId = createGenre(chiefEditor, "IT Workflow Genre " + suffix).genreId();
        String title = "IT Workflow Manuscript " + suffix;

        ManuscriptResponse submitted = submitManuscript(author, title, genreId);
        UUID manuscriptId = submitted.manuscriptId();

        assertThat(submitted.status()).isEqualTo(ManuscriptStatus.SUBMITTED);
        assertThat(dbString("select status from manuscripts where manuscript_id = ?", manuscriptId))
                .isEqualTo(ManuscriptStatus.SUBMITTED.name());
        assertThat(dbCount(
                "select count(*) from manuscript_genres where manuscript_id = ? and genre_id = ?",
                manuscriptId, genreId))
                .isEqualTo(1);

        awaitNotification(author, manuscriptId, "Рукопис подано");
        assertThat(dbCount(
                "select count(*) from notifications where recipient_id = ? and target_id = ? "
                        + "and title = ? and is_read = false",
                AUTHOR_ID, manuscriptId, "Рукопис подано"))
                .isEqualTo(1);

        ManuscriptResponse approved = approveManuscript(chiefEditor, manuscriptId, EDITOR_ID);

        assertThat(approved.status()).isEqualTo(ManuscriptStatus.IN_PROGRESS);
        assertThat(dbString("select status from manuscripts where manuscript_id = ?", manuscriptId))
                .isEqualTo(ManuscriptStatus.IN_PROGRESS.name());
        assertThat(dbCount(
                "select count(*) from team_assignments "
                        + "where manuscript_id = ? and user_id = ? and role = 'EDITOR'",
                manuscriptId, EDITOR_ID))
                .isEqualTo(1);

        awaitNotification(author, manuscriptId, "Рукопис схвалено");
        awaitNotification(author, manuscriptId, "Оновлення команди проєкту");
        awaitNotification(editor, manuscriptId, "Нове призначення");

        ContractResponse contract = awaitContract(author, manuscriptId);
        assertThat(contract.status()).isEqualTo(ContractStatus.DRAFT);
        assertThat(contract.authorId()).isEqualTo(AUTHOR_ID);
        assertThat(contract.title()).isEqualTo(title);
        assertThat(dbString("select status from contracts where contract_id = ?", contract.contractId()))
                .isEqualTo(ContractStatus.DRAFT.name());

        ChapterResponse chapter = createChapter(author, manuscriptId, "IT Chapter One", 1);
        ChapterResponse updatedChapter = updateChapter(
                author, chapter.chapterId(), "IT Chapter One Revised", 1);

        assertThat(updatedChapter.chapterTitle()).isEqualTo("IT Chapter One Revised");
        assertThat(dbString(
                "select chapter_title from chapters where chapter_id = ?", chapter.chapterId()))
                .isEqualTo("IT Chapter One Revised");

        ManuscriptResponse textApproved = finalizeText(editor, manuscriptId);

        assertThat(textApproved.status()).isEqualTo(ManuscriptStatus.TEXT_APPROVED);
        assertThat(dbString("select status from manuscripts where manuscript_id = ?", manuscriptId))
                .isEqualTo(ManuscriptStatus.TEXT_APPROVED.name());
        assertThat(dbCount(
                "select count(*) from manuscript_audit_logs where manuscript_id = ? "
                        + "and changed_by_user_id = ? and old_status = 'IN_PROGRESS' "
                        + "and new_status = 'TEXT_APPROVED'",
                manuscriptId, EDITOR_ID))
                .isEqualTo(1);
        awaitNotification(author, manuscriptId, "Редагування тексту завершено");

        ManuscriptResponse inDesign = assignDesigner(chiefEditor, manuscriptId, DESIGNER_ID);

        assertThat(inDesign.status()).isEqualTo(ManuscriptStatus.IN_DESIGN);
        assertThat(dbString("select status from manuscripts where manuscript_id = ?", manuscriptId))
                .isEqualTo(ManuscriptStatus.IN_DESIGN.name());
        assertThat(dbCount(
                "select count(*) from team_assignments "
                        + "where manuscript_id = ? and user_id = ? and role = 'DESIGNER'",
                manuscriptId, DESIGNER_ID))
                .isEqualTo(1);
        assertThat(dbCount(
                "select count(*) from manuscript_audit_logs where manuscript_id = ? "
                        + "and changed_by_user_id = ? and old_status = 'TEXT_APPROVED' "
                        + "and new_status = 'IN_DESIGN'",
                manuscriptId, CHIEF_EDITOR_ID))
                .isEqualTo(1);
        awaitNotification(designer, manuscriptId, "Нове призначення");

        CoverVersionResponse cover = uploadCover(designer, manuscriptId);

        assertThat(cover.versionNumber()).isEqualTo(1);
        assertThat(cover.isApproved()).isFalse();
        assertThat(dbCount(
                "select count(*) from cover_versions where id = ? and manuscript_id = ? "
                        + "and uploaded_by_user_id = ? and is_approved = false",
                cover.id(), manuscriptId, DESIGNER_ID))
                .isEqualTo(1);
        awaitNotification(author, manuscriptId, "Нова версія обкладинки");

        CoverVersionResponse approvedCover = approveCover(author, manuscriptId, 1);
        assertThat(approvedCover.isApproved()).isTrue();
        assertThat(dbBoolean("select is_approved from cover_versions where id = ?", cover.id()))
                .isTrue();

        BigDecimal royaltyPercent = new BigDecimal("17.50");
        BigDecimal advancePayment = new BigDecimal("3000.00");
        String reason = "IT workflow royalty renegotiation " + suffix;

        ContractResponse withRoyalty = updateRoyalty(
                accountant, contract.contractId(), royaltyPercent, advancePayment, reason);

        assertThat(withRoyalty.royaltyPercent()).isEqualByComparingTo(royaltyPercent);
        assertThat(withRoyalty.advancePayment()).isEqualByComparingTo(advancePayment);
        assertThat(dbDecimal(
                "select royalty_percent from contracts where contract_id = ?", contract.contractId()))
                .isEqualByComparingTo(royaltyPercent);
        assertThat(dbDecimal(
                "select advance_payment from contracts where contract_id = ?", contract.contractId()))
                .isEqualByComparingTo(advancePayment);
        assertThat(dbCount(
                "select count(*) from finance_audit_logs where contract_id = ? "
                        + "and changed_by_user_id = ? and change_reason = ?",
                contract.contractId(), ACCOUNTANT_ID, reason))
                .isEqualTo(1);
        awaitNotification(author, manuscriptId, "Оновлення відсотка роялті");

        ContractResponse confirmed = confirmContract(author, contract.contractId());

        assertThat(confirmed.authorConfirmedAt()).isNotNull();
        assertThat(dbCount(
                "select count(*) from contracts where contract_id = ? and author_confirmed_at is not null",
                contract.contractId()))
                .isEqualTo(1);
        awaitNotification(author, manuscriptId, "Контракт підтверджено");

        ManuscriptResponse published = publish(chiefEditor, manuscriptId);

        assertThat(published.status()).isEqualTo(ManuscriptStatus.PUBLISHED);
        assertThat(dbString("select status from manuscripts where manuscript_id = ?", manuscriptId))
                .isEqualTo(ManuscriptStatus.PUBLISHED.name());
        assertThat(dbCount(
                "select count(*) from manuscript_audit_logs where manuscript_id = ? "
                        + "and changed_by_user_id = ? and old_status = 'IN_DESIGN' "
                        + "and new_status = 'PUBLISHED'",
                manuscriptId, CHIEF_EDITOR_ID))
                .isEqualTo(1);
        awaitNotification(author, manuscriptId, "Книгу опубліковано!");
        awaitContractStatus(contract.contractId(), ContractStatus.ACTIVE);

        ContractResponse activeContract = getContractById(author, contract.contractId());
        assertThat(activeContract.status()).isEqualTo(ContractStatus.ACTIVE);

        assertThat(auditLog(chiefEditor, manuscriptId))
                .extracting(ManuscriptAuditLogResponse::newStatus)
                .contains(
                        ManuscriptStatus.TEXT_APPROVED,
                        ManuscriptStatus.IN_DESIGN,
                        ManuscriptStatus.PUBLISHED
                );
        assertThat(dbCount(
                "select count(*) from manuscript_audit_logs where manuscript_id = ?", manuscriptId))
                .isEqualTo(3);

        assertThat(myManuscripts(author))
                .filteredOn(manuscript -> manuscriptId.equals(manuscript.manuscriptId()))
                .singleElement()
                .satisfies(manuscript -> assertThat(manuscript.status())
                        .isEqualTo(ManuscriptStatus.PUBLISHED));
        assertThat(manuscriptsByStatus(chiefEditor, ManuscriptStatus.PUBLISHED))
                .extracting(ManuscriptResponse::manuscriptId)
                .contains(manuscriptId);
        assertThat(getManuscript(chiefEditor, manuscriptId).status())
                .isEqualTo(ManuscriptStatus.PUBLISHED);
    }
}
