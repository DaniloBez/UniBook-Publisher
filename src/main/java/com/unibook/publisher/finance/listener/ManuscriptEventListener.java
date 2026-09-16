package com.unibook.publisher.finance.listener;

import com.unibook.publisher.common.event.ManuscriptApprovedEvent;
import com.unibook.publisher.common.event.ManuscriptPublishedEvent;
import com.unibook.publisher.finance.service.ContractService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class ManuscriptEventListener {
    private final ContractService contractService;

    public ManuscriptEventListener(ContractService contractService) {
        this.contractService = contractService;
    }

    @EventListener
    public void onManuscriptApproved(ManuscriptApprovedEvent event) {
        contractService.createContractForApprovedManuscript(event);
    }

    //before this event is even published, the sender has to check whether the contract is currently confirmed by author
    //REM we are getting into some crazy ping-pong with that cascade of requests
    @EventListener
    public void onManuscriptPublished(ManuscriptPublishedEvent event) {
        contractService.activateContractForPublishedManuscript(event);
    }
}
