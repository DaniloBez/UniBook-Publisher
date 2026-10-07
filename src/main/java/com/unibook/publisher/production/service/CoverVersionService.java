package com.unibook.publisher.production.service;

import com.unibook.publisher.production.entity.response.CoverVersionResponse;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;

public interface CoverVersionService {
    CoverVersionResponse uploadCoverVersion(
            UUID manuscriptId,
            UUID designerId,
            InputStream fileStream,
            long size,
            String contentType,
            String originalFilename
    );
    List<CoverVersionResponse> getCoverVersions(UUID manuscriptId);
}
