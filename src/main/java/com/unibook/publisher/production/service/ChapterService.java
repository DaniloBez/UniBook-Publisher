package com.unibook.publisher.production.service;

import com.unibook.publisher.common.security.AuthenticatedUser;
import com.unibook.publisher.production.entity.request.ChapterCreationRequest;
import com.unibook.publisher.production.entity.request.ChapterUpdateRequest;
import com.unibook.publisher.production.entity.request.DiffRequest;
import com.unibook.publisher.production.entity.response.ChapterResponse;
import com.unibook.publisher.production.entity.response.DiffResponse;
import com.unibook.publisher.production.entity.response.RevisionResponse;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;

public interface ChapterService {
    ChapterResponse createChapter(UUID manuscriptId, AuthenticatedUser currentUser, ChapterCreationRequest request);

    ChapterResponse updateChapter(UUID chapterId, AuthenticatedUser currentUser, ChapterUpdateRequest request);

    void deleteChapter(UUID chapterId, AuthenticatedUser currentUser);

    List<ChapterResponse> getChaptersByManuscriptId(UUID manuscriptId);

    RevisionResponse uploadRevision(
            UUID chapterId,
            AuthenticatedUser currentUser,
            InputStream fileStream,
            long size,
            String contentType,
            String originalFilename
    );

    List<RevisionResponse> getRevisionsByChapterId(UUID chapterId);

    DiffResponse getDiffChapter(UUID chapterId, DiffRequest request);
}
