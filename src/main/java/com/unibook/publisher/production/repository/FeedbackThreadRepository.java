package com.unibook.publisher.production.repository;

import com.unibook.publisher.production.entity.FeedbackThread;
import com.unibook.publisher.production.enums.ThreadStatus;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class FeedbackThreadRepository {
    private final ConcurrentHashMap<UUID, FeedbackThread> threads = new ConcurrentHashMap<>();

    public FeedbackThread save(FeedbackThread thread) {
        threads.put(thread.id(), thread);
        return thread;
    }

    public Optional<FeedbackThread> findById(UUID id) {
        return Optional.ofNullable(threads.get(id));
    }

    public List<FeedbackThread> findByChapterId(UUID chapterId) {
        return threads.values().stream()
                .filter(thread -> thread.chapterId().equals(chapterId))
                .sorted(Comparator.comparing(FeedbackThread::createdAt))
                .toList();
    }

    public List<FeedbackThread> findByChapterIdAndStatus(UUID chapterId, ThreadStatus status) {
        return threads.values().stream()
                .filter(thread -> thread.chapterId().equals(chapterId) && thread.status() == status)
                .sorted(Comparator.comparing(FeedbackThread::createdAt))
                .toList();
    }
}
