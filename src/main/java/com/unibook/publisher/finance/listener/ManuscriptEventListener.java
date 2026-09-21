package com.unibook.publisher.finance.listener;

import com.unibook.publisher.common.logging.AppLogger;
import com.unibook.publisher.common.event.ManuscriptApprovedEvent;
import com.unibook.publisher.common.event.ManuscriptPublishedEvent;
import com.unibook.publisher.finance.service.ContractService;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component
public class ManuscriptEventListener {
    private final ContractService contractService;
    private final AppLogger logger;

    public ManuscriptEventListener(ContractService contractService, AppLogger logger) {
        this.contractService = contractService;
        this.logger = logger;
    }

    @ApplicationModuleListener
    public void onManuscriptApproved(ManuscriptApprovedEvent event) {
        logger.info("Received event ManuscriptApprovedEvent: manuscriptId={}, authorId={}, editorId={}", event.manuscriptId(), event.authorId(), event.editorId());
        contractService.createContractForApprovedManuscript(event);
    }

    //before this event is even published, the sender has to check whether the contract is currently confirmed by author
    //REM we are getting into some crazy ping-pong with that cascade of requests
    @ApplicationModuleListener
    public void onManuscriptPublished(ManuscriptPublishedEvent event) {
        logger.info("Received event ManuscriptPublishedEvent: manuscriptId={}, authorId={}", event.manuscriptId(), event.authorId());
        contractService.activateContractForPublishedManuscript(event);
    }
}
