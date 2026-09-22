package com.unibook.publisher.production.service;

import com.unibook.publisher.production.entity.request.CoverVersionRequest;
import com.unibook.publisher.production.entity.response.CoverVersionResponse;

import java.util.List;
import java.util.UUID;

public interface CoverVersionService {

    CoverVersionResponse uploadCoverVersion(UUID manuscriptId, UUID designerId, CoverVersionRequest request);

    List<CoverVersionResponse> getCoverVersions(UUID manuscriptId);
}
