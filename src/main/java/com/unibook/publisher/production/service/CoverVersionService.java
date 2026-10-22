package com.unibook.publisher.production.service;

import com.unibook.publisher.common.security.AuthenticatedUser;
import com.unibook.publisher.production.entity.response.CoverVersionResponse;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;

public interface CoverVersionService {
    CoverVersionResponse uploadCoverVersion(
            UUID manuscriptId,
            AuthenticatedUser currentUser,
            InputStream fileStream,
            long size,
            String contentType,
            String originalFilename
    );

    CoverVersionResponse approveCoverVersion(
            UUID manuscriptId,
            UUID coverVersionId,
            AuthenticatedUser currentUser);

    List<CoverVersionResponse> getCoverVersions(UUID manuscriptId);
}
