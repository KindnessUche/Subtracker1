package com.subscription_tracker.uchekd.repository;

import com.subscription_tracker.uchekd.model.Subscription;
import com.subscription_tracker.uchekd.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {

    List<Subscription> findByUser(User user);

    Optional<Subscription> findByIdAndUser(UUID id, User user);

    /** Used to dedup on approve so the same merchant isn't added twice. Case-insensitive. */
    Optional<Subscription> findFirstByUserAndMerchantNameIgnoreCase(User user, String merchantName);

    /**
     * Narrower dedup match: same merchant AND same billing cycle. Preferred over the
     * merchant-only lookup above when the billing cycle is known, since a user can have two
     * distinct subscriptions from the same merchant (e.g. two Adobe products on different
     * cycles) that would otherwise incorrectly collide.
     */
    Optional<Subscription> findFirstByUserAndMerchantNameIgnoreCaseAndBillingCycle(
            User user, String merchantName, String billingCycle);

    /** Active subscriptions whose billing date has already passed — need their date rolled forward. */
    List<Subscription> findByStatusAndNextBillingDateBefore(String status, LocalDate date);

    /** Active subscriptions renewing on or before the given cutoff — candidates for a reminder email. */
    List<Subscription> findByStatusAndNextBillingDateLessThanEqual(String status, LocalDate cutoff);
}