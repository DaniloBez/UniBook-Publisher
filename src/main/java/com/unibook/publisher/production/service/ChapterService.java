package com.unibook.publisher.production.service;

import com.unibook.publisher.production.entity.request.ChapterCreationRequest;
import com.unibook.publisher.production.entity.request.RevisionUploadRequest;
import com.unibook.publisher.production.entity.response.ChapterResponse;
import com.unibook.publisher.production.entity.response.RevisionResponse;

import java.util.List;
import java.util.UUID;

public interface ChapterService {

    ChapterResponse createChapter(UUID manuscriptId, UUID authorId, ChapterCreationRequest request);

    List<ChapterResponse> getChaptersByManuscriptId(UUID manuscriptId);

    RevisionResponse uploadRevision(UUID chapterId, UUID userId, RevisionUploadRequest request);

    List<RevisionResponse> getRevisionsByChapterId(UUID chapterId);
}
