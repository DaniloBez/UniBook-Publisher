package com.unibook.publisher.production.service;

import com.unibook.publisher.identity.entity.User;
import com.unibook.publisher.production.entity.request.OpenThreadRequest;
import com.unibook.publisher.production.entity.request.ThreadMessageRequest;
import com.unibook.publisher.production.entity.response.ThreadMessageResponse;
import com.unibook.publisher.production.entity.response.ThreadResponse;
import com.unibook.publisher.production.enums.ThreadStatus;

import java.util.List;
import java.util.UUID;

public interface FeedbackThreadService {
    ThreadResponse openThread(UUID chapterId, User currentUser, OpenThreadRequest request);
    List<ThreadResponse> getThreads(UUID chapterId, ThreadStatus statusFilter);
    ThreadMessageResponse addMessage(UUID threadId, User currentUser, ThreadMessageRequest request);
    ThreadResponse acceptSuggestion(UUID threadId, User currentUser);
    ThreadResponse rejectSuggestion(UUID threadId, User currentUser);
    ThreadResponse resolveThread(UUID threadId, User currentUser);
    void deleteThread(UUID threadId, User currentUser);
}
