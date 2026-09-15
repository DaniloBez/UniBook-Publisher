package com.unibook.publisher.production.repository;

import java.util.UUID;
import org.springframework.stereotype.Repository;
import com.unibook.publisher.production.enums.ThreadStatus;

@Repository 
public class FeedbackThreadRepository {
    public void findByChapterId(UUID chapterId) {

    } 
    
    public void findByChapterIdAndStatus(UUID chapterId, ThreadStatus status) {

    }
}
