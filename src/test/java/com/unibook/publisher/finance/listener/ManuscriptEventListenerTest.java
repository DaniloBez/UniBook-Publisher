package com.unibook.publisher.finance.listener;

import com.unibook.publisher.common.event.ManuscriptApprovedEvent;
import com.unibook.publisher.common.event.ManuscriptPublishedEvent;
import com.unibook.publisher.finance.service.ContractService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ManuscriptEventListenerTest {

    @Mock
    private ContractService contractService;

    @InjectMocks
    private ManuscriptEventListener listener;

    @Test
    @DisplayName("onManuscriptApproved: делегує створення контракту сервісу")
    void onManuscriptApproved_DelegatesToService() {
        ManuscriptApprovedEvent event = new ManuscriptApprovedEvent(
                UUID.randomUUID(), "Назва твору", UUID.randomUUID(), UUID.randomUUID()
        );

        listener.onManuscriptApproved(event);

        verify(contractService).createContractForApprovedManuscript(event);
    }

    @Test
    @DisplayName("onManuscriptPublished: делегує активацію контракту сервісу")
    void onManuscriptPublished_DelegatesToService() {
        ManuscriptPublishedEvent event = new ManuscriptPublishedEvent(
                UUID.randomUUID(), "Назва твору", UUID.randomUUID()
        );

        listener.onManuscriptPublished(event);

        verify(contractService).activateContractForPublishedManuscript(event);
    }
}
