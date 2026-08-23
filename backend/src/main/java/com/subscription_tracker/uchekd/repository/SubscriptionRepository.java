package com.subscription_tracker.uchekd.repository;

import com.subscription_tracker.uchekd.model.Subscription;
import com.subscription_tracker.uchekd.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {

    List<Subscription> findByUser(User user);

    Optional<Subscription> findByIdAndUser(UUID id, User user);

    /** Used to dedup on approve so the same merchant isn't added twice. Case-insensitive. */
    Optional<Subscription> findFirstByUserAndMerchantNameIgnoreCase(User user, String merchantName);
}