package com.unibook.publisher.production.service;

import com.unibook.publisher.common.security.AuthenticatedUser;
import com.unibook.publisher.production.entity.request.OpenThreadRequest;
import com.unibook.publisher.production.entity.request.ThreadMessageRequest;
import com.unibook.publisher.production.entity.response.ThreadMessageResponse;
import com.unibook.publisher.production.entity.response.ThreadResponse;
import com.unibook.publisher.production.enums.ThreadStatus;

import java.util.List;
import java.util.UUID;

public interface FeedbackThreadService {
    ThreadResponse openThread(UUID chapterId, AuthenticatedUser currentUser, OpenThreadRequest request);
    List<ThreadResponse> getThreads(UUID chapterId, ThreadStatus statusFilter);
    ThreadMessageResponse addMessage(UUID threadId, AuthenticatedUser currentUser, ThreadMessageRequest request);
    ThreadResponse acceptSuggestion(UUID threadId, AuthenticatedUser currentUser);
    ThreadResponse rejectSuggestion(UUID threadId, AuthenticatedUser currentUser);
    ThreadResponse resolveThread(UUID threadId, AuthenticatedUser currentUser);
    void deleteThread(UUID threadId, AuthenticatedUser currentUser);
}
