package com.unibook.publisher.integration;

import com.unibook.publisher.common.enums.ContractStatus;
import com.unibook.publisher.finance.entity.request.ContractUpdateRequest;
import com.unibook.publisher.finance.entity.response.ContractResponse;
import com.unibook.publisher.identity.entity.response.AuthResponse;
import com.unibook.publisher.integration.support.ApiIntegrationTestSupport;
import com.unibook.publisher.production.entity.response.ManuscriptResponse;
import com.unibook.publisher.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class ContractApiIntegrationTest extends ApiIntegrationTestSupport {

    @Test
    void contract_ShouldBeCreatedAsyncReadUpdatedAndConfirmed() throws Exception {
        AuthResponse author = login(AUTHOR_EMAIL);
        AuthResponse chiefEditor = login(CHIEF_EDITOR_EMAIL);
        AuthResponse accountant = login(ACCOUNTANT_EMAIL);
        UUID genreId = createGenre(chiefEditor, "IT Genre " + UUID.randomUUID()).genreId();
        String title = "IT Contract Manuscript " + UUID.randomUUID();

        ManuscriptResponse inProgress = submitAndApprove(author, chiefEditor, genreId, title);
        ContractResponse draft = awaitContract(author, inProgress.manuscriptId());

        assertThat(draft.status()).isEqualTo(ContractStatus.DRAFT);
        assertThat(draft.manuscriptId()).isEqualTo(inProgress.manuscriptId());
        assertThat(draft.title()).isEqualTo(title);
        assertThat(draft.authorId()).isEqualTo(AUTHOR_ID);
        assertThat(draft.royaltyPercent()).isEqualByComparingTo("0.00");
        assertThat(draft.advancePayment()).isEqualByComparingTo("0.00");
        assertThat(draft.authorConfirmedAt()).isNull();

        assertThat(dbString("select status from contracts where contract_id = ?", draft.contractId()))
                .isEqualTo(ContractStatus.DRAFT.name());
        assertThat(dbUuid("select manuscript_id from contracts where contract_id = ?", draft.contractId()))
                .isEqualTo(inProgress.manuscriptId());
        assertThat(dbUuid("select author_id from contracts where contract_id = ?", draft.contractId()))
                .isEqualTo(AUTHOR_ID);

        ContractResponse byId = getContractById(author, draft.contractId());
        assertThat(byId.contractId()).isEqualTo(draft.contractId());

        BigDecimal updatedRoyalty = new BigDecimal("17.50");
        BigDecimal updatedAdvance = new BigDecimal("3000.00");
        String reason = "Integration test renegotiation " + UUID.randomUUID();

        ContractResponse updated = updateRoyalty(
                accountant, draft.contractId(), updatedRoyalty, updatedAdvance, reason);

        assertThat(updated.royaltyPercent()).isEqualByComparingTo(updatedRoyalty);
        assertThat(updated.advancePayment()).isEqualByComparingTo(updatedAdvance);
        assertThat(dbDecimal(
                "select royalty_percent from contracts where contract_id = ?", draft.contractId()))
                .isEqualByComparingTo(updatedRoyalty);
        assertThat(dbDecimal(
                "select advance_payment from contracts where contract_id = ?", draft.contractId()))
                .isEqualByComparingTo(updatedAdvance);
        assertThat(dbCount(
                "select count(*) from finance_audit_logs where contract_id = ? "
                        + "and changed_by_user_id = ? and change_reason = ?",
                draft.contractId(), ACCOUNTANT_ID, reason))
                .isEqualTo(1);
        assertThat(dbDecimal(
                "select new_royalty_percent from finance_audit_logs where contract_id = ?",
                draft.contractId()))
                .isEqualByComparingTo(updatedRoyalty);

        BigDecimal replacedRoyalty = new BigDecimal("18.25");
        BigDecimal replacedAdvance = new BigDecimal("3500.00");
        mockMvc.perform(authorized(put("/api/v1/contracts/{id}", draft.contractId()), accountant)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ContractUpdateRequest(
                                replacedRoyalty,
                                replacedAdvance
                        ))))
                .andExpect(status().isOk());

        assertThat(dbDecimal(
                "select royalty_percent from contracts where contract_id = ?", draft.contractId()))
                .isEqualByComparingTo(replacedRoyalty);
        assertThat(dbDecimal(
                "select advance_payment from contracts where contract_id = ?", draft.contractId()))
                .isEqualByComparingTo(replacedAdvance);
        assertThat(dbCount(
                "select count(*) from contracts where contract_id = ? and author_confirmed_at is null",
                draft.contractId()))
                .isEqualTo(1);

        ContractResponse confirmed = confirmContract(author, draft.contractId());

        assertThat(confirmed.authorConfirmedAt()).isNotNull();
        assertThat(dbCount(
                "select count(*) from contracts where contract_id = ? and author_confirmed_at is not null",
                draft.contractId()))
                .isEqualTo(1);
    }
}
