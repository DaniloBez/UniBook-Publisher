package com.unibook.publisher.production.repository;

import com.unibook.publisher.production.entity.ThreadMessage;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class ThreadMessageRepository {
    private final ConcurrentHashMap<UUID, ThreadMessage> messages = new ConcurrentHashMap<>();

    public ThreadMessage save(ThreadMessage message) {
        messages.put(message.id(), message);
        return message;
    }

    public Optional<ThreadMessage> findById(UUID id) {
        return Optional.ofNullable(messages.get(id));
    }

    public List<ThreadMessage> findByThreadId(UUID threadId) {
        return messages.values().stream()
                .filter(message -> message.threadId().equals(threadId))
                .sorted(Comparator.comparing(ThreadMessage::sentAt))
                .toList();
    }
}
