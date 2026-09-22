package com.unibook.publisher.production.service;

import com.unibook.publisher.common.enums.ThreadType;
import com.unibook.publisher.common.enums.UserRole;
import com.unibook.publisher.common.event.ThreadMessageAddedEvent;
import com.unibook.publisher.common.event.ThreadOpenedEvent;
import com.unibook.publisher.common.exception.business.ThreadNotASuggestionException;
import com.unibook.publisher.common.exception.notfound.ChapterNotFoundException;
import com.unibook.publisher.common.exception.notfound.ManuscriptNotFoundException;
import com.unibook.publisher.common.exception.notfound.ThreadNotFoundException;
import com.unibook.publisher.common.exception.security.ForbiddenActionException;
import com.unibook.publisher.common.exception.state.InvalidStateTransitionException;
import com.unibook.publisher.common.logging.AppLogger;
import com.unibook.publisher.production.entity.Chapter;
import com.unibook.publisher.production.entity.FeedbackThread;
import com.unibook.publisher.production.entity.Manuscript;
import com.unibook.publisher.production.entity.TeamAssignment;
import com.unibook.publisher.production.entity.ThreadMessage;
import com.unibook.publisher.production.entity.request.OpenThreadRequest;
import com.unibook.publisher.production.entity.request.ThreadMessageRequest;
import com.unibook.publisher.production.entity.response.ThreadMessageResponse;
import com.unibook.publisher.production.entity.response.ThreadResponse;
import com.unibook.publisher.production.enums.SuggestionStatus;
import com.unibook.publisher.production.enums.ThreadStatus;
import com.unibook.publisher.production.repository.ChapterRepository;
import com.unibook.publisher.production.repository.FeedbackThreadRepository;
import com.unibook.publisher.production.repository.ManuscriptRepository;
import com.unibook.publisher.production.repository.TeamAssignmentRepository;
import com.unibook.publisher.production.repository.ThreadMessageRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class FeedbackThreadServiceImpl implements FeedbackThreadService {
    private final FeedbackThreadRepository threadRepository;
    private final ThreadMessageRepository messageRepository;
    private final ChapterRepository chapterRepository;
    private final ManuscriptRepository manuscriptRepository;
    private final TeamAssignmentRepository teamAssignmentRepository;
    private final ApplicationEventPublisher publisher;
    private final AppLogger logger;

    public FeedbackThreadServiceImpl(
            FeedbackThreadRepository threadRepository,
            ThreadMessageRepository messageRepository,
            ChapterRepository chapterRepository,
            ManuscriptRepository manuscriptRepository,
            TeamAssignmentRepository teamAssignmentRepository,
            ApplicationEventPublisher publisher,
            AppLogger logger
    ) {
        this.threadRepository = threadRepository;
        this.messageRepository = messageRepository;
        this.chapterRepository = chapterRepository;
        this.manuscriptRepository = manuscriptRepository;
        this.teamAssignmentRepository = teamAssignmentRepository;
        this.publisher = publisher;
        this.logger = logger;
    }

    @Override
    public ThreadResponse openThread(UUID chapterId, UUID initiatorId, OpenThreadRequest request) {
        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new ChapterNotFoundException(chapterId));

        Manuscript manuscript = manuscriptRepository.findById(chapter.manuscriptId())
                .orElseThrow(() -> new ManuscriptNotFoundException(chapter.manuscriptId()));

        Optional<TeamAssignment> editor = teamAssignmentRepository.findByManuscriptIdAndRole(manuscript.manuscriptId(), UserRole.EDITOR);
        UUID recipientId = resolveOtherParty(manuscript, editor, initiatorId);

        boolean isSuggestion = request.suggestedText() != null && !request.suggestedText().isBlank();
        FeedbackThread thread = new FeedbackThread(
                UUID.randomUUID(),
                chapterId,
                initiatorId,
                ThreadStatus.OPEN,
                isSuggestion,
                isSuggestion ? request.suggestedText() : null,
                isSuggestion ? SuggestionStatus.PENDING : null,
                Instant.now()
        );
        threadRepository.save(thread);

        logger.info(
                "Created feedback thread {} for chapter {} by user {}",
                thread.id(),
                chapterId,
                initiatorId
        );

        ThreadMessage message = messageRepository.save(new ThreadMessage(
                UUID.randomUUID(),
                thread.id(),
                initiatorId,
                request.initialMessage(),
                Instant.now()
        ));

        logger.info(
                "Created thread message {} in thread {} by user {}",
                message.id(),
                thread.id(),
                initiatorId
        );

        publisher.publishEvent(new ThreadOpenedEvent(
                manuscript.manuscriptId(),
                manuscript.title(),
                ThreadType.CHAPTER,
                thread.id(),
                chapterId,
                initiatorId,
                recipientId,
                chapter.chapterTitle()
        ));

        return ThreadResponse.from(thread);
    }

    @Override
    public List<ThreadResponse> getThreads(UUID chapterId, ThreadStatus statusFilter) {
        if (chapterRepository.findById(chapterId).isEmpty())
            throw new ChapterNotFoundException(chapterId);

        List<FeedbackThread> threads = statusFilter == null
                ? threadRepository.findByChapterId(chapterId)
                : threadRepository.findByChapterIdAndStatus(chapterId, statusFilter);
        return threads.stream().map(ThreadResponse::from).toList();
    }

    @Override
    public ThreadMessageResponse addMessage(UUID threadId, UUID senderId, ThreadMessageRequest request) {
        FeedbackThread thread = threadRepository.findById(threadId)
                .orElseThrow(() -> new ThreadNotFoundException(threadId));

        Chapter chapter = chapterRepository.findById(thread.chapterId())
                .orElseThrow(() -> new ChapterNotFoundException(thread.chapterId()));

        Manuscript manuscript = manuscriptRepository.findById(chapter.manuscriptId())
                .orElseThrow(() -> new ManuscriptNotFoundException(chapter.manuscriptId()));

        Optional<TeamAssignment> editor = teamAssignmentRepository.findByManuscriptIdAndRole(manuscript.manuscriptId(), UserRole.EDITOR);

        if (!isAuthorOrEditor(manuscript, editor, senderId))
            throw new ForbiddenActionException("Відповідати в треді може лише автор або призначений редактор");


        ThreadMessage saved = messageRepository.save(new ThreadMessage(
                UUID.randomUUID(),
                threadId,
                senderId,
                request.content(),
                Instant.now()
        ));

        UUID recipientId = resolveOtherParty(manuscript, editor, senderId);
        publisher.publishEvent(new ThreadMessageAddedEvent(
                manuscript.manuscriptId(),
                threadId,
                chapter.chapterTitle(),
                senderId,
                recipientId,
                request.content()
        ));

        return ThreadMessageResponse.from(saved);
    }

    @Override
    public ThreadResponse acceptSuggestion(UUID threadId, UUID userId) {
        FeedbackThread thread = threadRepository.findById(threadId)
                .orElseThrow(() -> new ThreadNotFoundException(threadId));
        Manuscript manuscript = manuscriptOfThread(thread);

        if (!manuscript.authorId().equals(userId))
            throw new ForbiddenActionException("Пропозицію може прийняти лише автор рукопису");

        if (!thread.isSuggestion())
            throw new ThreadNotASuggestionException();

        if (thread.suggestionStatus() != SuggestionStatus.PENDING) {
            throw new InvalidStateTransitionException(
                    "Suggestion",
                    threadId,
                    thread.suggestionStatus(),
                    SuggestionStatus.ACCEPTED,
                    Set.of()
            );
        }

        FeedbackThread updated = new FeedbackThread(
                thread.id(), thread.chapterId(), thread.createdByUserId(), thread.status(),
                thread.isSuggestion(), thread.suggestedText(), SuggestionStatus.ACCEPTED, thread.createdAt()
        );
        threadRepository.save(updated);

        logger.info(
                "Updated suggestion status for thread {}: {} -> ACCEPTED by user {}",
                threadId,
                thread.suggestionStatus(),
                userId
        );

        return ThreadResponse.from(updated);
    }

    @Override
    public ThreadResponse rejectSuggestion(UUID threadId, UUID userId) {
        FeedbackThread thread = threadRepository.findById(threadId)
                .orElseThrow(() -> new ThreadNotFoundException(threadId));
        Manuscript manuscript = manuscriptOfThread(thread);

        if (!manuscript.authorId().equals(userId))
            throw new ForbiddenActionException("Пропозицію може відхилити лише автор рукопису");

        if (!thread.isSuggestion())
            throw new ThreadNotASuggestionException();

        if (thread.suggestionStatus() != SuggestionStatus.PENDING) {
            throw new InvalidStateTransitionException(
                    "Suggestion",
                    threadId,
                    thread.suggestionStatus(),
                    SuggestionStatus.REJECTED,
                    Set.of()
            );
        }

        FeedbackThread updated = new FeedbackThread(
                thread.id(), thread.chapterId(), thread.createdByUserId(), thread.status(),
                thread.isSuggestion(), thread.suggestedText(), SuggestionStatus.REJECTED, thread.createdAt()
        );
        threadRepository.save(updated);

        logger.info(
                "Updated suggestion status for thread {}: {} -> REJECTED by user {}",
                threadId,
                thread.suggestionStatus(),
                userId
        );

        return ThreadResponse.from(updated);
    }

    @Override
    public ThreadResponse resolveThread(UUID threadId, UUID userId) {
        FeedbackThread thread = threadRepository.findById(threadId)
                .orElseThrow(() -> new ThreadNotFoundException(threadId));

        Manuscript manuscript = manuscriptOfThread(thread);
        Optional<TeamAssignment> editor = teamAssignmentRepository.findByManuscriptIdAndRole(manuscript.manuscriptId(), UserRole.EDITOR);

        if (!isAuthorOrEditor(manuscript, editor, userId))
            throw new ForbiddenActionException("Закрити тред може лише автор або призначений редактор");

        if (thread.status() == ThreadStatus.RESOLVED) {
            throw new InvalidStateTransitionException(
                    "Thread",
                    threadId,
                    ThreadStatus.RESOLVED,
                    ThreadStatus.RESOLVED,
                    Set.of()
            );
        }

        FeedbackThread updated = new FeedbackThread(
                thread.id(), thread.chapterId(), thread.createdByUserId(), ThreadStatus.RESOLVED,
                thread.isSuggestion(), thread.suggestedText(), thread.suggestionStatus(), thread.createdAt()
        );
        threadRepository.save(updated);

        logger.info(
                "Updated thread {} status: {} -> RESOLVED by user {}",
                threadId,
                thread.status(),
                userId
        );

        return ThreadResponse.from(updated);
    }

    private Manuscript manuscriptOfThread(FeedbackThread thread) {
        Chapter chapter = chapterRepository.findById(thread.chapterId())
                .orElseThrow(() -> new ChapterNotFoundException(thread.chapterId()));

        return manuscriptRepository.findById(chapter.manuscriptId())
                .orElseThrow(() -> new ManuscriptNotFoundException(chapter.manuscriptId()));
    }

    private boolean isAuthorOrEditor(Manuscript manuscript, Optional<TeamAssignment> editor, UUID userId) {
        return manuscript.authorId().equals(userId)
                || editor.map(a -> a.userId().equals(userId)).orElse(false);
    }

    private UUID resolveOtherParty(Manuscript manuscript, Optional<TeamAssignment> editor, UUID userId) {
        if (manuscript.authorId().equals(userId))
            return editor.map(TeamAssignment::userId).orElse(null);

        return manuscript.authorId();
    }
}
