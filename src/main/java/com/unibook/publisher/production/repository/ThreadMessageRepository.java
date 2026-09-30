package com.unibook.publisher.production.repository;

import com.unibook.publisher.production.entity.ThreadMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ThreadMessageRepository extends JpaRepository<ThreadMessage, UUID> {
    List<ThreadMessage> findByThread_IdOrderBySentAtAsc(UUID threadId);
}
