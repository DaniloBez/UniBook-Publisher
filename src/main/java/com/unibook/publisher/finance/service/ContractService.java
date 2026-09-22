package com.unibook.publisher.finance.service;

import com.unibook.publisher.common.enums.UserRole;
import com.unibook.publisher.common.event.ManuscriptApprovedEvent;
import com.unibook.publisher.common.event.ManuscriptPublishedEvent;
import com.unibook.publisher.finance.entity.request.PayoutSimulationRequest;
import com.unibook.publisher.finance.entity.request.RoyaltyUpdateRequest;
import com.unibook.publisher.finance.entity.response.ContractResponse;
import com.unibook.publisher.finance.entity.response.PayoutSimulationResponse;

import java.util.UUID;

public interface ContractService {

    void createContractForApprovedManuscript(ManuscriptApprovedEvent event);

    void activateContractForPublishedManuscript(ManuscriptPublishedEvent event);

    ContractResponse getContractByManuscriptId(UUID manuscriptId, UUID callerId, UserRole callerRole);

    ContractResponse updateRoyalty(UUID contractId, UUID callerId, UserRole callerRole, RoyaltyUpdateRequest request);

    ContractResponse confirmContract(UUID contractId, UUID callerId);

    PayoutSimulationResponse simulatePayout(UUID contractId, UUID callerId, UserRole callerRole, PayoutSimulationRequest request);
}
