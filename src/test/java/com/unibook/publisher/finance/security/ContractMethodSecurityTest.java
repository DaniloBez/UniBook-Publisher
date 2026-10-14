package com.unibook.publisher.finance.security;

import com.unibook.publisher.common.enums.UserRole;
import com.unibook.publisher.common.security.AuthenticatedUser;
import com.unibook.publisher.common.logging.AppLogger;
import com.unibook.publisher.finance.entity.request.ContractUpdateRequest;
import com.unibook.publisher.finance.entity.request.RoyaltyUpdateRequest;
import com.unibook.publisher.finance.repository.ContractRepository;
import com.unibook.publisher.finance.repository.FinanceAuditLogRepository;
import com.unibook.publisher.finance.service.ContractService;
import com.unibook.publisher.finance.service.ContractServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static com.unibook.publisher.testutil.TestUsers.asUser;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/**
 * Role checks on the service layer are declared with @PreAuthorize, so they only run
 * when a Spring Security method-security proxy is present (plain Mockito unit tests skip them).
 */
@SpringJUnitConfig(ContractMethodSecurityTest.Config.class)
class ContractMethodSecurityTest {

    @Configuration
    @EnableMethodSecurity
    static class Config {
        @Bean
        ContractService contractService() {
            return new ContractServiceImpl(
                    mock(ContractRepository.class),
                    mock(FinanceAuditLogRepository.class),
                    mock(ApplicationEventPublisher.class),
                    mock(AppLogger.class),
                    List.of()
            );
        }
    }

    @Autowired
    private ContractService contractService;

    private final UUID id = UUID.randomUUID();

    @Test
    @WithMockUser(roles = "AUTHOR")
    @DisplayName("getAllContracts: AUTHOR отримує AccessDenied")
    void getAllContracts_Author_Denied() {
        assertThatThrownBy(() -> contractService.getAllContracts()).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @WithMockUser(roles = "EDITOR")
    @DisplayName("getAllContracts: EDITOR отримує AccessDenied")
    void getAllContracts_Editor_Denied() {
        assertThatThrownBy(() -> contractService.getAllContracts()).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("updateContract: ADMIN отримує AccessDenied")
    void updateContract_Admin_Denied() {
        ContractUpdateRequest request = new ContractUpdateRequest(new BigDecimal("20.0"), new BigDecimal("3000.0"));
        assertThatThrownBy(() -> contractService.updateContract(id, request)).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @WithMockUser(roles = "ACCOUNTANT")
    @DisplayName("deleteContract: ACCOUNTANT отримує AccessDenied")
    void deleteContract_Accountant_Denied() {
        assertThatThrownBy(() -> contractService.deleteContract(id)).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @WithMockUser(roles = "EDITOR")
    @DisplayName("updateRoyalty: EDITOR отримує AccessDenied")
    void updateRoyalty_Editor_Denied() {
        RoyaltyUpdateRequest request = new RoyaltyUpdateRequest(new BigDecimal("12.5"), new BigDecimal("5000.0"), "Причина");
        AuthenticatedUser randomEditorUser = asUser(UUID.randomUUID(), UserRole.EDITOR);
        assertThatThrownBy(() -> contractService.updateRoyalty(id, randomEditorUser, request))
                .isInstanceOf(AccessDeniedException.class);
    }
}
