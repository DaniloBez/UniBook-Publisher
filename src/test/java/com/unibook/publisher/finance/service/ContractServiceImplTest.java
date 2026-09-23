package com.unibook.publisher.finance.service;

import com.unibook.publisher.common.enums.UserRole;
import com.unibook.publisher.common.enums.RoyaltyStrategyType;
import com.unibook.publisher.common.event.ContractConfirmedEvent;
import com.unibook.publisher.common.event.ContractRoyaltyUpdatedEvent;
import com.unibook.publisher.common.event.ManuscriptApprovedEvent;
import com.unibook.publisher.common.event.ManuscriptPublishedEvent;
import com.unibook.publisher.common.exception.business.UnsupportedRoyaltyStrategyException;
import com.unibook.publisher.common.exception.notfound.ContractNotFoundException;
import com.unibook.publisher.common.exception.notfound.ResourceNotFoundException;
import com.unibook.publisher.common.exception.security.ForbiddenActionException;
import com.unibook.publisher.common.exception.state.InvalidStateTransitionException;
import com.unibook.publisher.common.logging.AppLogger;
import com.unibook.publisher.finance.entity.Contract;
import com.unibook.publisher.common.enums.ContractStatus;
import com.unibook.publisher.finance.entity.request.PayoutSimulationRequest;
import com.unibook.publisher.finance.entity.request.RoyaltyUpdateRequest;
import com.unibook.publisher.finance.entity.response.ContractResponse;
import com.unibook.publisher.finance.entity.response.PayoutSimulationResponse;
import com.unibook.publisher.finance.repository.ContractRepository;
import com.unibook.publisher.finance.repository.FinanceAuditLogRepository;
import com.unibook.publisher.finance.royalty.RoyaltyStrategy;
import com.unibook.publisher.finance.royalty.impl.FlatRateRoyaltyStrategy;
import com.unibook.publisher.finance.royalty.impl.TieredVolumeRoyaltyStrategy;
import com.unibook.publisher.finance.royalty.impl.AdvanceRecoupmentRoyaltyStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContractServiceImplTest {

    @Mock
    private ContractRepository contractRepository;

    @Mock
    private FinanceAuditLogRepository financeAuditLogRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private AppLogger logger;

    @Mock
    private RoyaltyStrategy flatRateRoyaltyStrategy;

    @Mock
    private RoyaltyStrategy tieredVolumeRoyaltyStrategy;

    @Mock
    private RoyaltyStrategy advanceRecoupmentRoyaltyStrategy;

    private ContractServiceImpl contractService;
    @BeforeEach
    void setUp() {
        when(flatRateRoyaltyStrategy.getType()).thenReturn(RoyaltyStrategyType.FLAT_RATE);
        when(tieredVolumeRoyaltyStrategy.getType()).thenReturn(RoyaltyStrategyType.TIERED_VOLUME);
        when(advanceRecoupmentRoyaltyStrategy.getType()).thenReturn(RoyaltyStrategyType.ADVANCE_RECOUPMENT);

        contractService = new ContractServiceImpl(
                contractRepository,
                financeAuditLogRepository,
                eventPublisher,
                logger,
                List.of(
                        flatRateRoyaltyStrategy,
                        tieredVolumeRoyaltyStrategy,
                        advanceRecoupmentRoyaltyStrategy
                )
        );
    }

    private final UUID manuscriptId = UUID.randomUUID();
    private final UUID authorId = UUID.randomUUID();
    private final UUID contractId = UUID.randomUUID();
    private final String manuscriptTitle = "Кобзар";  //REM ironically, I don't think that we need to strike a contracted deal with Shevchenko

    private Contract draftContract() {
        return new Contract(
                contractId,
                manuscriptId,
                manuscriptTitle,
                authorId,
                new BigDecimal("10.0"),
                new BigDecimal("1000.0"),
                ContractStatus.DRAFT,
                null,
                Instant.now()
        );
    }

    @Nested
    @DisplayName("Створення та активація контракту через події")
    class LifecycleEventTests {

        @Test
        @DisplayName("createContractForApprovedManuscript: створює новий контракт у статусі DRAFT")
        void createContractForApprovedManuscript_Success() {
            ManuscriptApprovedEvent event = new ManuscriptApprovedEvent(manuscriptId, manuscriptTitle, UUID.randomUUID(), authorId);
            when(contractRepository.save(any(Contract.class))).thenAnswer(invocation -> invocation.getArgument(0)); //returning the very same input

            contractService.createContractForApprovedManuscript(event);

            ArgumentCaptor<Contract> captor = ArgumentCaptor.forClass(Contract.class);
            verify(contractRepository).save(captor.capture());

            Contract saved = captor.getValue();
            assertThat(saved.manuscriptId()).isEqualTo(manuscriptId);
            assertThat(saved.authorId()).isEqualTo(authorId);
            assertThat(saved.status()).isEqualTo(ContractStatus.DRAFT);
            assertThat(saved.authorConfirmedAt()).isNull();
        }

        @Test
        @DisplayName("activateContractForPublishedManuscript: переводить контракт у статус ACTIVE")
        void activateContractForPublishedManuscript_Success() {
            Contract confirmedDraft = draftContract().confirmedByAuthor(Instant.now());
            ManuscriptPublishedEvent event = new ManuscriptPublishedEvent(manuscriptId, manuscriptTitle, authorId);

            when(contractRepository.findByManuscriptId(manuscriptId)).thenReturn(Optional.of(confirmedDraft));
            when(contractRepository.save(any(Contract.class))).thenAnswer(invocation -> invocation.getArgument(0));

            contractService.activateContractForPublishedManuscript(event);

            ArgumentCaptor<Contract> captor = ArgumentCaptor.forClass(Contract.class);
            verify(contractRepository).save(captor.capture());
            assertThat(captor.getValue().status()).isEqualTo(ContractStatus.ACTIVE);
        }

        @Test
        @DisplayName("activateContractForPublishedManuscript: помилка, якщо контракт не знайдено")
        void activateContractForPublishedManuscript_NotFound_ThrowsException() {
            ManuscriptPublishedEvent event = new ManuscriptPublishedEvent(manuscriptId, manuscriptTitle, authorId);
            when(contractRepository.findByManuscriptId(manuscriptId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> contractService.activateContractForPublishedManuscript(event))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Перегляд контракту")
    class ViewAccessTests {

        @Test
        @DisplayName("Бухгалтер може переглянути будь-який контракт")
        void getContract_AsAccountant_Success() {
            when(contractRepository.findByManuscriptId(manuscriptId)).thenReturn(Optional.of(draftContract()));

            ContractResponse response = contractService.getContractByManuscriptId(manuscriptId, UUID.randomUUID(), UserRole.ACCOUNTANT);

            assertThat(response.manuscriptId()).isEqualTo(manuscriptId);
        }

        @Test
        @DisplayName("Автор-власник може переглянути свій контракт")
        void getContract_AsOwnerAuthor_Success() {
            when(contractRepository.findByManuscriptId(manuscriptId)).thenReturn(Optional.of(draftContract()));

            ContractResponse response = contractService.getContractByManuscriptId(manuscriptId, authorId, UserRole.AUTHOR);

            assertThat(response.authorId()).isEqualTo(authorId);
        }

        @Test
        @DisplayName("Сторонній автор не може переглянути чужий контракт")
        void getContract_AsForeignAuthor_ThrowsException() {
            when(contractRepository.findByManuscriptId(manuscriptId)).thenReturn(Optional.of(draftContract()));

            assertThatThrownBy(() -> contractService.getContractByManuscriptId(manuscriptId, UUID.randomUUID(), UserRole.AUTHOR)) //REM I would die of laughing if that random UUID would match author id and that would fail the correct scenario. I swear, this will happen before the most important deploy to production
                    .isInstanceOf(ForbiddenActionException.class);
        }

        @Test
        @DisplayName("Адміністратор може переглянути будь-який контракт")
        void getContract_AsAdmin_Success() {
            when(contractRepository.findByManuscriptId(manuscriptId)).thenReturn(Optional.of(draftContract()));

            ContractResponse response = contractService.getContractByManuscriptId(manuscriptId, UUID.randomUUID(), UserRole.ADMIN);

            assertThat(response.manuscriptId()).isEqualTo(manuscriptId);
        }

        @Test
        @DisplayName("Помилка, якщо контракт не знайдено за id рукопису")
        void getContract_ContractNotFound_ThrowsException() {
            when(contractRepository.findByManuscriptId(manuscriptId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> contractService.getContractByManuscriptId(manuscriptId, UUID.randomUUID(), UserRole.ACCOUNTANT))
                    .isInstanceOf(ContractNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Зміна ставки роялті")
    class UpdateRoyaltyTests {

        @Test
        @DisplayName("Бухгалтер успішно змінює ставку в статусі DRAFT та скидає підтвердження автора")
        void updateRoyalty_Success_ResetsConfirmation() {
            Contract confirmed = draftContract().confirmedByAuthor(Instant.now());
            RoyaltyUpdateRequest request = new RoyaltyUpdateRequest(
                    new BigDecimal("12.5"), new BigDecimal("5000.0"), "Перегляд з огляду на обсяг рукопису"
            );

            when(contractRepository.findById(contractId)).thenReturn(Optional.of(confirmed));
            when(contractRepository.save(any(Contract.class))).thenAnswer(invocation -> invocation.getArgument(0));

            ContractResponse response = contractService.updateRoyalty(contractId, UUID.randomUUID(), UserRole.ACCOUNTANT, request);

            assertThat(response.royaltyPercent()).isEqualByComparingTo("12.5");
            assertThat(response.advancePayment()).isEqualByComparingTo("5000.0");
            assertThat(response.authorConfirmedAt()).isNull();

            verify(financeAuditLogRepository).save(any());
            verify(eventPublisher).publishEvent(any(ContractRoyaltyUpdatedEvent.class));
        }

        @Test
        @DisplayName("Помилка, якщо роль викликача не ACCOUNTANT") //REM You shall not pass!!(Gandalf, LOTR)
        void updateRoyalty_NotAccountant_ThrowsException() {
            RoyaltyUpdateRequest request = new RoyaltyUpdateRequest(
                    new BigDecimal("12.5"), new BigDecimal("5000.0"), "Причина"
            );

            assertThatThrownBy(() -> contractService.updateRoyalty(contractId, UUID.randomUUID(), UserRole.EDITOR, request))
                    .isInstanceOf(ForbiddenActionException.class);

            verifyNoInteractions(contractRepository, financeAuditLogRepository, eventPublisher);
        }

        @Test
        @DisplayName("Помилка, якщо контракт вже у статусі ACTIVE")
        void updateRoyalty_ActiveContract_ThrowsException() {
            Contract active = draftContract().activated();
            RoyaltyUpdateRequest request = new RoyaltyUpdateRequest(
                    new BigDecimal("12.5"), new BigDecimal("5000.0"), "Причина"
            );

            when(contractRepository.findById(contractId)).thenReturn(Optional.of(active));

            assertThatThrownBy(() -> contractService.updateRoyalty(contractId, UUID.randomUUID(), UserRole.ACCOUNTANT, request))
                    .isInstanceOf(InvalidStateTransitionException.class);

            verify(financeAuditLogRepository, never()).save(any());
            verifyNoInteractions(eventPublisher);
        }

        @Test
        @DisplayName("Помилка, якщо контракт не знайдено")
        void updateRoyalty_ContractNotFound_ThrowsException() {
            RoyaltyUpdateRequest request = new RoyaltyUpdateRequest(
                    new BigDecimal("12.5"), new BigDecimal("5000.0"), "Причина"
            );

            when(contractRepository.findById(contractId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> contractService.updateRoyalty(contractId, UUID.randomUUID(), UserRole.ACCOUNTANT, request))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Підтвердження контракту автором")
    class ConfirmContractTests {

        @Test
        @DisplayName("Автор успішно підтверджує контракт у статусі DRAFT")
        void confirmContract_Success() {
            Contract draft = draftContract();

            when(contractRepository.findById(contractId)).thenReturn(Optional.of(draft));
            when(contractRepository.save(any(Contract.class))).thenAnswer(invocation -> invocation.getArgument(0));

            ContractResponse response = contractService.confirmContract(contractId, authorId);

            assertThat(response.authorConfirmedAt()).isNotNull();
            assertThat(response.status()).isEqualTo(ContractStatus.DRAFT);

            verify(eventPublisher).publishEvent(any(ContractConfirmedEvent.class));
        }

        @Test
        @DisplayName("Помилка, якщо підтверджує не автор-власник")
        void confirmContract_NotOwner_ThrowsException() {
            Contract draft = draftContract();
            when(contractRepository.findById(contractId)).thenReturn(Optional.of(draft));

            assertThatThrownBy(() -> contractService.confirmContract(contractId, UUID.randomUUID()))
                    .isInstanceOf(ForbiddenActionException.class);

            verify(contractRepository, never()).save(any());
            verifyNoInteractions(eventPublisher);
        }

        @Test
        @DisplayName("Помилка, якщо контракт вже ACTIVE")
        void confirmContract_AlreadyActive_ThrowsException() {
            Contract active = draftContract().activated();
            when(contractRepository.findById(contractId)).thenReturn(Optional.of(active));

            assertThatThrownBy(() -> contractService.confirmContract(contractId, authorId))
                    .isInstanceOf(InvalidStateTransitionException.class);
        }

        @Test
        @DisplayName("Повторне підтвердження вже підтвердженого контракту нічого не змінює (ідемпотентність)")
        void confirmContract_AlreadyConfirmed_IsIdempotent() {
            Contract alreadyConfirmed = draftContract().confirmedByAuthor(Instant.now());
            when(contractRepository.findById(contractId)).thenReturn(Optional.of(alreadyConfirmed));

            ContractResponse response = contractService.confirmContract(contractId, authorId);

            assertThat(response.authorConfirmedAt()).isEqualTo(alreadyConfirmed.authorConfirmedAt());
            verify(contractRepository, never()).save(any());
            verifyNoInteractions(eventPublisher);
        }
    }

    @Nested
    @DisplayName("Симуляція розрахунку роялті")
    class SimulatePayoutTests {

        @Test
        @DisplayName("Коректний розрахунок виплати на основі ставки та авансу")
        void simulatePayout_Success() {
            Contract contract = draftContract();
            BigDecimal salesAmount = new BigDecimal("10000.0");

            when(contractRepository.findById(contractId)).thenReturn(Optional.of(contract));

            when(flatRateRoyaltyStrategy.calculateRoyalty(contract, salesAmount))
                    .thenReturn(new BigDecimal("1000.00"));

            when(tieredVolumeRoyaltyStrategy.calculateRoyalty(contract, salesAmount))
                    .thenReturn(new BigDecimal("800.00"));

            when(advanceRecoupmentRoyaltyStrategy.calculateRoyalty(contract, salesAmount))
                    .thenReturn(new BigDecimal("0.00"));

            PayoutSimulationRequest flatRateRequest = new PayoutSimulationRequest(
                    salesAmount,
                    RoyaltyStrategyType.FLAT_RATE
            );

            PayoutSimulationResponse flatRateResponse = contractService.simulatePayout(
                    contractId,
                    authorId,
                    UserRole.AUTHOR,
                    flatRateRequest
            );

            assertThat(flatRateResponse.calculatedRoyalty()).isEqualByComparingTo("1000.00");
            assertThat(flatRateResponse.totalPayout()).isEqualByComparingTo("2000.00");

            PayoutSimulationRequest tieredRequest = new PayoutSimulationRequest(
                    salesAmount,
                    RoyaltyStrategyType.TIERED_VOLUME
            );

            PayoutSimulationResponse tieredResponse = contractService.simulatePayout(
                    contractId,
                    authorId,
                    UserRole.AUTHOR,
                    tieredRequest
            );

            assertThat(tieredResponse.calculatedRoyalty()).isEqualByComparingTo("800.00");
            assertThat(tieredResponse.totalPayout()).isEqualByComparingTo("1800.00");

            PayoutSimulationRequest advanceRecoupmentRequest = new PayoutSimulationRequest(
                    salesAmount,
                    RoyaltyStrategyType.ADVANCE_RECOUPMENT
            );

            PayoutSimulationResponse advanceRecoupmentResponse = contractService.simulatePayout(
                    contractId,
                    authorId,
                    UserRole.AUTHOR,
                    advanceRecoupmentRequest
            );

            assertThat(advanceRecoupmentResponse.calculatedRoyalty()).isEqualByComparingTo("0.00");
            assertThat(advanceRecoupmentResponse.totalPayout()).isEqualByComparingTo("1000.00");

            verify(flatRateRoyaltyStrategy).calculateRoyalty(contract, salesAmount);
            verify(tieredVolumeRoyaltyStrategy).calculateRoyalty(contract, salesAmount);
            verify(advanceRecoupmentRoyaltyStrategy).calculateRoyalty(contract, salesAmount);
        }

        @Test
        @DisplayName("Помилка доступу для стороннього користувача")
        void simulatePayout_Forbidden_ThrowsException() {
            Contract contract = draftContract();
            PayoutSimulationRequest request = new PayoutSimulationRequest(new BigDecimal("10000.0"), RoyaltyStrategyType.FLAT_RATE);

            when(contractRepository.findById(contractId)).thenReturn(Optional.of(contract));

            assertThatThrownBy(() -> contractService.simulatePayout(contractId, UUID.randomUUID(), UserRole.EDITOR, request))
                    .isInstanceOf(ForbiddenActionException.class);
        }

        @Test
        @DisplayName("Помилка, якщо контракт не знайдено")
        void simulatePayout_ContractNotFound_ThrowsException() {
            PayoutSimulationRequest request = new PayoutSimulationRequest(new BigDecimal("10000.0"), RoyaltyStrategyType.FLAT_RATE);

            when(contractRepository.findById(contractId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> contractService.simulatePayout(contractId, authorId, UserRole.AUTHOR, request))
                    .isInstanceOf(ContractNotFoundException.class);
        }

        @Test
        @DisplayName("Помилка, якщо для типу стратегії роялті немає зареєстрованої реалізації")
        void simulatePayout_UnsupportedRoyaltyStrategy_ThrowsException() {
            Contract contract = draftContract();
            PayoutSimulationRequest request = new PayoutSimulationRequest(new BigDecimal("10000.0"), RoyaltyStrategyType.ADVANCE_RECOUPMENT);

            // Simulate a deployment where the ADVANCE_RECOUPMENT strategy bean is not registered:
            // resolveStrategy must throw UnsupportedRoyaltyStrategyException instead of silently
            // falling back to another strategy.
            ContractServiceImpl serviceWithMissingStrategy = new ContractServiceImpl(
                    contractRepository,
                    financeAuditLogRepository,
                    eventPublisher,
                    logger,
                    List.of(flatRateRoyaltyStrategy, tieredVolumeRoyaltyStrategy)
            );

            when(contractRepository.findById(contractId)).thenReturn(Optional.of(contract));

            assertThatThrownBy(() -> serviceWithMissingStrategy.simulatePayout(contractId, authorId, UserRole.AUTHOR, request))
                    .isInstanceOf(UnsupportedRoyaltyStrategyException.class);
        }
    }
}
