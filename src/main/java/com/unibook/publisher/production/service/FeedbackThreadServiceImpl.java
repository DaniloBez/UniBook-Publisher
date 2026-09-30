package com.unibook.publisher.production.service;

import com.unibook.publisher.common.enums.ThreadType;
import com.unibook.publisher.common.enums.UserRole;
import com.unibook.publisher.common.event.ThreadMessageAddedEvent;
import com.unibook.publisher.common.event.ThreadOpenedEvent;
import com.unibook.publisher.common.exception.business.EmptyRevisionTextException;
import com.unibook.publisher.common.exception.business.InvalidQuoteException;
import com.unibook.publisher.common.exception.business.InvalidQuotePositionException;
import com.unibook.publisher.common.exception.business.ThreadNotASuggestionException;
import com.unibook.publisher.common.exception.notfound.ChapterNotFoundException;
import com.unibook.publisher.common.exception.notfound.ManuscriptNotFoundException;
import com.unibook.publisher.common.exception.notfound.RevisionNotFoundException;
import com.unibook.publisher.common.exception.notfound.ThreadNotFoundException;
import com.unibook.publisher.common.exception.security.ForbiddenActionException;
import com.unibook.publisher.common.exception.state.InvalidStateTransitionException;
import com.unibook.publisher.common.logging.AppLogger;
import com.unibook.publisher.production.entity.*;
import com.unibook.publisher.production.entity.request.OpenThreadRequest;
import com.unibook.publisher.production.entity.request.ThreadMessageRequest;
import com.unibook.publisher.production.entity.response.ThreadMessageResponse;
import com.unibook.publisher.production.entity.response.ThreadResponse;
import com.unibook.publisher.production.enums.SuggestionStatus;
import com.unibook.publisher.production.enums.ThreadStatus;
import com.unibook.publisher.production.repository.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class FeedbackThreadServiceImpl implements FeedbackThreadService {
    private final FeedbackThreadRepository threadRepository;
    private final ThreadMessageRepository messageRepository;
    private final ChapterRepository chapterRepository;
    private final RevisionRepository revisionRepository;
    private final ManuscriptRepository manuscriptRepository;
    private final TeamAssignmentRepository teamAssignmentRepository;
    private final ApplicationEventPublisher publisher;
    private final AppLogger logger;

    public FeedbackThreadServiceImpl(
            FeedbackThreadRepository threadRepository,
            ThreadMessageRepository messageRepository,
            ChapterRepository chapterRepository, RevisionRepository revisionRepository,
            ManuscriptRepository manuscriptRepository,
            TeamAssignmentRepository teamAssignmentRepository,
            ApplicationEventPublisher publisher,
            AppLogger logger
    ) {
        this.threadRepository = threadRepository;
        this.messageRepository = messageRepository;
        this.chapterRepository = chapterRepository;
        this.revisionRepository = revisionRepository;
        this.manuscriptRepository = manuscriptRepository;
        this.teamAssignmentRepository = teamAssignmentRepository;
        this.publisher = publisher;
        this.logger = logger;
    }

    @Override
    @Transactional
    public ThreadResponse openThread(UUID chapterId, UUID initiatorId, OpenThreadRequest request) {
        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new ChapterNotFoundException(chapterId));

        Manuscript manuscript = chapter.getManuscript();

        validateQuote(request);
        boolean isSuggestion = request.suggestedText() != null && !request.suggestedText().isBlank();

        Optional<TeamAssignment> editor = teamAssignmentRepository.findByManuscript_ManuscriptIdAndRole(manuscript.getManuscriptId(), UserRole.EDITOR);
        UUID recipientId = resolveOtherParty(manuscript, editor, initiatorId);

                FeedbackThread thread = new FeedbackThread();
        thread.setCreatedByUserId(initiatorId);
        thread.setStatus(ThreadStatus.OPEN);
        thread.setSuggestion(isSuggestion);
        thread.setSuggestedText(isSuggestion ? request.suggestedText() : null);
        thread.setSuggestionStatus(isSuggestion ? SuggestionStatus.PENDING : null);
        thread.setCreatedAt(Instant.now());
        thread.setTargetRevisionId(request.targetRevisionId());
        thread.setQuotedText(request.quotedText());
        thread.setPositionFrom(request.positionFrom());
        thread.setPositionTo(request.positionTo());
        chapter.addThread(thread);

        ThreadMessage message = new ThreadMessage(initiatorId, request.initialMessage(), Instant.now());
        thread.addMessage(message);
        threadRepository.save(thread);

        logger.info(
                "Created feedback thread {} with initial message {} for chapter {} by user {}",
                thread.getId(),
                message.getId(),
                chapterId,
                initiatorId
        );

        publisher.publishEvent(new ThreadOpenedEvent(
                manuscript.getManuscriptId(),
                manuscript.getTitle(),
                ThreadType.CHAPTER,
                thread.getId(),
                chapterId,
                initiatorId,
                recipientId,
                chapter.getChapterTitle()
        ));

        return ThreadResponse.from(thread);
    }

    private void validateQuote(OpenThreadRequest request) {
        if (request.targetRevisionId() == null || request.quotedText() == null) {
            return;
        }

        UUID revisionId = request.targetRevisionId();
        Revision revision = revisionRepository.findById(revisionId)
                .orElseThrow(() -> new RevisionNotFoundException(revisionId));

        String textContent = revision.getTextContent();
        if (textContent == null) {
            throw new EmptyRevisionTextException();
        }

        Integer from = request.positionFrom();
        Integer to = request.positionTo();
        if (from == null || to == null || from < 0 || to > textContent.length() || from >= to) {
            throw new InvalidQuotePositionException();
        }

        String quote = textContent.substring(from, to);
        if (!quote.equals(request.quotedText())) {
            throw new InvalidQuoteException(request.quotedText());
        }
    }

    @Override
    public List<ThreadResponse> getThreads(UUID chapterId, ThreadStatus statusFilter) {
        if (chapterRepository.findById(chapterId).isEmpty())
            throw new ChapterNotFoundException(chapterId);

        List<FeedbackThread> threads = statusFilter == null
                ? threadRepository.findByChapterIdWithMessages(chapterId)
                : threadRepository.findByChapterIdAndStatusWithMessages(chapterId, statusFilter);
        return threads.stream().map(ThreadResponse::from).toList();
    }

    @Override
    @Transactional
    public ThreadMessageResponse addMessage(UUID threadId, UUID senderId, ThreadMessageRequest request) {
        FeedbackThread thread = threadRepository.findById(threadId)
                .orElseThrow(() -> new ThreadNotFoundException(threadId));

        Chapter chapter = thread.getChapter();
        Manuscript manuscript = chapter.getManuscript();

        Optional<TeamAssignment> editor = teamAssignmentRepository.findByManuscript_ManuscriptIdAndRole(manuscript.getManuscriptId(), UserRole.EDITOR);

        if (!isAuthorOrEditor(manuscript, editor, senderId))
            throw new ForbiddenActionException("Відповідати в треді може лише автор або призначений редактор");


        ThreadMessage message = new ThreadMessage(senderId, request.content(), Instant.now());
        message.setThread(thread);
        ThreadMessage saved = messageRepository.save(message);

        UUID recipientId = resolveOtherParty(manuscript, editor, senderId);
        publisher.publishEvent(new ThreadMessageAddedEvent(
                manuscript.getManuscriptId(),
                threadId,
                chapter.getChapterTitle(),
                senderId,
                recipientId,
                request.content()
        ));

        return ThreadMessageResponse.from(saved);
    }

    @Override
    @Transactional
    public ThreadResponse acceptSuggestion(UUID threadId, UUID userId) {
        FeedbackThread thread = threadRepository.findById(threadId)
                .orElseThrow(() -> new ThreadNotFoundException(threadId));
        Manuscript manuscript = manuscriptOfThread(thread);

        if (!manuscript.getAuthorId().equals(userId))
            throw new ForbiddenActionException("Пропозицію може прийняти лише автор рукопису");

        if (!thread.isSuggestion())
            throw new ThreadNotASuggestionException();

        if (thread.getSuggestionStatus() != SuggestionStatus.PENDING) {
            throw new InvalidStateTransitionException(
                    "Suggestion",
                    threadId,
                    thread.getSuggestionStatus(),
                    SuggestionStatus.ACCEPTED,
                    Set.of()
            );
        }

        SuggestionStatus oldStatus = thread.getSuggestionStatus();
        thread.setSuggestionStatus(SuggestionStatus.ACCEPTED);
        FeedbackThread updated = threadRepository.save(thread);

        logger.info(
                "Updated suggestion status for thread {}: {} -> ACCEPTED by user {}",
                threadId,
                oldStatus,
                userId
        );

        return ThreadResponse.from(updated);
    }

    @Override
    @Transactional
    public ThreadResponse rejectSuggestion(UUID threadId, UUID userId) {
        FeedbackThread thread = threadRepository.findById(threadId)
                .orElseThrow(() -> new ThreadNotFoundException(threadId));
        Manuscript manuscript = manuscriptOfThread(thread);

        if (!manuscript.getAuthorId().equals(userId))
            throw new ForbiddenActionException("Пропозицію може відхилити лише автор рукопису");

        if (!thread.isSuggestion())
            throw new ThreadNotASuggestionException();

        if (thread.getSuggestionStatus() != SuggestionStatus.PENDING) {
            throw new InvalidStateTransitionException(
                    "Suggestion",
                    threadId,
                    thread.getSuggestionStatus(),
                    SuggestionStatus.REJECTED,
                    Set.of()
            );
        }

        SuggestionStatus oldStatus = thread.getSuggestionStatus();
        thread.setSuggestionStatus(SuggestionStatus.REJECTED);
        FeedbackThread updated = threadRepository.save(thread);

        logger.info(
                "Updated suggestion status for thread {}: {} -> REJECTED by user {}",
                threadId,
                oldStatus,
                userId
        );

        return ThreadResponse.from(updated);
    }

    @Override
    @Transactional
    public ThreadResponse resolveThread(UUID threadId, UUID userId) {
        FeedbackThread thread = threadRepository.findById(threadId)
                .orElseThrow(() -> new ThreadNotFoundException(threadId));

        Manuscript manuscript = manuscriptOfThread(thread);
        Optional<TeamAssignment> editor = teamAssignmentRepository.findByManuscript_ManuscriptIdAndRole(manuscript.getManuscriptId(), UserRole.EDITOR);

        if (!isAuthorOrEditor(manuscript, editor, userId))
            throw new ForbiddenActionException("Закрити тред може лише автор або призначений редактор");

        if (thread.getStatus() == ThreadStatus.RESOLVED) {
            throw new InvalidStateTransitionException(
                    "Thread",
                    threadId,
                    ThreadStatus.RESOLVED,
                    ThreadStatus.RESOLVED,
                    Set.of()
            );
        }

        ThreadStatus oldStatus = thread.getStatus();
        thread.setStatus(ThreadStatus.RESOLVED);
        FeedbackThread updated = threadRepository.save(thread);

        logger.info(
                "Updated thread {} status: {} -> RESOLVED by user {}",
                threadId,
                oldStatus,
                userId
        );

        return ThreadResponse.from(updated);
    }

    private Manuscript manuscriptOfThread(FeedbackThread thread) {
        return thread.getChapter().getManuscript();
    }

    private boolean isAuthorOrEditor(Manuscript manuscript, Optional<TeamAssignment> editor, UUID userId) {
        return manuscript.getAuthorId().equals(userId)
                || editor.map(a -> a.getUserId().equals(userId)).orElse(false);
    }

    private UUID resolveOtherParty(Manuscript manuscript, Optional<TeamAssignment> editor, UUID userId) {
        if (manuscript.getAuthorId().equals(userId))
            return editor.map(TeamAssignment::getUserId).orElse(null);

        return manuscript.getAuthorId();
    }

    @Override
    @Transactional
    public void deleteThread(UUID threadId, UUID userId) {
        FeedbackThread thread = threadRepository.findById(threadId)
                .orElseThrow(() -> new ThreadNotFoundException(threadId));
        Manuscript manuscript = manuscriptOfThread(thread);
        Optional<TeamAssignment> editor = teamAssignmentRepository.findByManuscript_ManuscriptIdAndRole(manuscript.getManuscriptId(), UserRole.EDITOR);

        if (!isAuthorOrEditor(manuscript, editor, userId))
            throw new ForbiddenActionException("Видалити тред може лише автор або призначений редактор");

        threadRepository.delete(thread);

        logger.info("Deleted feedback thread {} by user {}", threadId, userId);
    }
}
