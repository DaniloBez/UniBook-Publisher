package com.unibook.publisher.production.service;

import com.unibook.publisher.production.entity.request.OpenThreadRequest;
import com.unibook.publisher.production.entity.request.ThreadMessageRequest;
import com.unibook.publisher.production.entity.response.ThreadMessageResponse;
import com.unibook.publisher.production.entity.response.ThreadResponse;
import com.unibook.publisher.production.enums.ThreadStatus;

import java.util.List;
import java.util.UUID;

public interface FeedbackThreadService {

    ThreadResponse openThread(UUID chapterId, UUID initiatorId, OpenThreadRequest request);

    List<ThreadResponse> getThreads(UUID chapterId, ThreadStatus statusFilter);

    ThreadMessageResponse addMessage(UUID threadId, UUID senderId, ThreadMessageRequest request);

    ThreadResponse acceptSuggestion(UUID threadId, UUID userId);

    ThreadResponse rejectSuggestion(UUID threadId, UUID userId);

    ThreadResponse resolveThread(UUID threadId, UUID userId);
}
