package com.unibook.publisher.finance.service;

import com.unibook.publisher.common.event.ManuscriptApprovedEvent;
import com.unibook.publisher.common.event.ManuscriptPublishedEvent;
import com.unibook.publisher.finance.entity.request.ContractUpdateRequest;
import com.unibook.publisher.finance.entity.request.PayoutSimulationRequest;
import com.unibook.publisher.finance.entity.request.RoyaltyUpdateRequest;
import com.unibook.publisher.finance.entity.response.ContractResponse;
import com.unibook.publisher.finance.entity.response.PayoutSimulationResponse;
import com.unibook.publisher.common.security.AuthenticatedUser;

import java.util.List;
import java.util.UUID;

public interface ContractService {
    void createContractForApprovedManuscript(ManuscriptApprovedEvent event);
    void activateContractForPublishedManuscript(ManuscriptPublishedEvent event);
    ContractResponse getContractByManuscriptId(UUID manuscriptId, AuthenticatedUser currentUser);
    ContractResponse updateRoyalty(UUID contractId, AuthenticatedUser currentUser, RoyaltyUpdateRequest request);
    ContractResponse confirmContract(UUID contractId, AuthenticatedUser currentUser);
    PayoutSimulationResponse simulatePayout(UUID contractId, AuthenticatedUser currentUser, PayoutSimulationRequest request);
    ContractResponse getContractById(UUID id, AuthenticatedUser currentUser);
    List<ContractResponse> getAllContracts();
    ContractResponse updateContract(UUID id, ContractUpdateRequest request);
    void deleteContract(UUID id);
}
