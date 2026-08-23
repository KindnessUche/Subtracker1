package com.subscription_tracker.uchekd.repository;

import com.subscription_tracker.uchekd.model.ReviewQueueItem;
import com.subscription_tracker.uchekd.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReviewQueueItemRepository extends JpaRepository<ReviewQueueItem, UUID> {
    List<ReviewQueueItem> findByUserAndStatusOrderByCreatedAtDesc(User user, String status);
    boolean existsByUserAndSourceMessageId(User user, String sourceMessageId);
    Optional<ReviewQueueItem> findByIdAndUser(UUID id, User user);
}