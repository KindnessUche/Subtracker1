package com.subscription_tracker.uchekd.controller;

import com.subscription_tracker.uchekd.dto.ReviewQueueItemResponse;
import com.subscription_tracker.uchekd.model.ReviewQueueItem;
import com.subscription_tracker.uchekd.model.Subscription;
import com.subscription_tracker.uchekd.model.User;
import com.subscription_tracker.uchekd.repository.ReviewQueueItemRepository;
import com.subscription_tracker.uchekd.repository.SubscriptionRepository;
import com.subscription_tracker.uchekd.repository.UserRepository;
import com.subscription_tracker.uchekd.service.ReviewQueueService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/review-queue")
public class ReviewQueueController {

    @Autowired private UserRepository userRepository;
    @Autowired private ReviewQueueItemRepository reviewQueueItemRepository;
    @Autowired private SubscriptionRepository subscriptionRepository;
    @Autowired private ReviewQueueService reviewQueueService;

    @PostMapping("/sync")
    public ResponseEntity<?> sync(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        try {
            ReviewQueueService.SyncResult result = reviewQueueService.syncAndQueue(user);
            return ResponseEntity.ok(Map.of(
                    "queued", result.queued(),
                    "scanned", result.scanned(),
                    "skipped", result.skipped(),
                    "failed", result.failed()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(502).body(Map.of("error", "Sync failed: " + e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<?> list(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        List<ReviewQueueItemResponse> items = reviewQueueItemRepository
                .findByUserAndStatusOrderByCreatedAtDesc(user, "PENDING")
                .stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(items);
    }

    @PostMapping("/{id}/approve")
    @Transactional
    public ResponseEntity<?> approve(@AuthenticationPrincipal UserDetails userDetails, @PathVariable UUID id) {
        User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        ReviewQueueItem item = reviewQueueItemRepository.findByIdAndUser(id, user).orElse(null);

        if (item == null) {
            return ResponseEntity.status(404).body(Map.of("error", "Not found"));
        }
        if (!"PENDING".equals(item.getStatus())) {
            return ResponseEntity.status(409).body(Map.of("error", "Already " + item.getStatus().toLowerCase()));
        }

        Subscription existing = subscriptionRepository
                .findFirstByUserAndMerchantNameIgnoreCase(user, item.getMerchantName())
                .orElse(null);

        if ("PRICE_CHANGE".equals(item.getType())) {
            // A price change should UPDATE the existing subscription, not create a second one.
            if (existing == null) {
                // No subscription to update — fall through and create it as a new one.
                existing = createSubscription(user, item);
            } else {
                existing.setAmount(item.getAmount());
                existing.setCurrency(item.getCurrency());
                if (item.getBillingCycle() != null && !"UNKNOWN".equals(item.getBillingCycle())) {
                    existing.setBillingCycle(item.getBillingCycle());
                }
                subscriptionRepository.save(existing);
            }
        } else {
            // NEW_SUBSCRIPTION: dedup so re-syncing or a second receipt doesn't duplicate the merchant.
            if (existing != null) {
                item.setStatus("APPROVED");
                reviewQueueItemRepository.save(item);
                return ResponseEntity.ok(Map.of(
                        "message", "Already tracking " + item.getMerchantName() + " — marked reviewed, no duplicate created",
                        "item", toResponse(item)
                ));
            }
            createSubscription(user, item);
        }

        item.setStatus("APPROVED");
        reviewQueueItemRepository.save(item);

        return ResponseEntity.ok(toResponse(item));
    }

    private Subscription createSubscription(User user, ReviewQueueItem item) {
        Subscription sub = new Subscription();
        sub.setUser(user);
        sub.setMerchantName(item.getMerchantName());
        sub.setAmount(item.getAmount());
        sub.setCurrency(item.getCurrency());
        sub.setBillingCycle(item.getBillingCycle());
        sub.setNextBillingDate(nextBillingDate(item.getBillingCycle()));
        sub.setStatus("ACTIVE");
        sub.setIsTrial(false);
        sub.setCreatedAt(Instant.now());
        subscriptionRepository.save(sub);
        return sub;
    }

    /** Derive the next billing date from the cycle instead of always assuming +1 month. */
    private LocalDate nextBillingDate(String billingCycle) {
        LocalDate today = LocalDate.now();
        if (billingCycle == null) {
            return today.plusMonths(1);
        }
        return switch (billingCycle) {
            case "WEEKLY" -> today.plusWeeks(1);
            case "QUARTERLY" -> today.plusMonths(3);
            case "ANNUAL" -> today.plusYears(1);
            case "MONTHLY" -> today.plusMonths(1);
            default -> today.plusMonths(1); // UNKNOWN — best-effort default
        };
    }

    @PostMapping("/{id}/dismiss")
    public ResponseEntity<?> dismiss(@AuthenticationPrincipal UserDetails userDetails, @PathVariable UUID id) {
        User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        ReviewQueueItem item = reviewQueueItemRepository.findByIdAndUser(id, user).orElse(null);

        if (item == null) {
            return ResponseEntity.status(404).body(Map.of("error", "Not found"));
        }

        item.setStatus("DISMISSED");
        reviewQueueItemRepository.save(item);

        return ResponseEntity.ok(toResponse(item));
    }

    private ReviewQueueItemResponse toResponse(ReviewQueueItem item) {
        return new ReviewQueueItemResponse(
                item.getId(),
                item.getType(),
                item.getMerchantName(),
                item.getAmount(),
                item.getCurrency(),
                item.getBillingCycle(),
                item.getPreviousAmount(),
                item.getConfidenceScore(),
                item.getRawSnippet(),
                item.getStatus(),
                item.getCreatedAt()
        );
    }
}