package com.subscription_tracker.uchekd.repository;

import com.subscription_tracker.uchekd.model.PriceHistory;
import com.subscription_tracker.uchekd.model.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PriceHistoryRepository extends JpaRepository<PriceHistory, UUID> {

    List<PriceHistory> findBySubscriptionOrderByEffectiveAtAsc(Subscription subscription);
}
