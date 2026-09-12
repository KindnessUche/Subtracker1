package com.subscription_tracker.uchekd.controller;

import com.subscription_tracker.uchekd.dto.SubscriptionRequest;
import com.subscription_tracker.uchekd.dto.SubscriptionResponse;
import com.subscription_tracker.uchekd.model.PriceHistory;
import com.subscription_tracker.uchekd.model.Subscription;
import com.subscription_tracker.uchekd.model.User;
import com.subscription_tracker.uchekd.repository.PriceHistoryRepository;
import com.subscription_tracker.uchekd.repository.SubscriptionRepository;
import com.subscription_tracker.uchekd.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/subscriptions")
public class SubscriptionController {

    @Autowired private SubscriptionRepository subscriptionRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PriceHistoryRepository priceHistoryRepository;

    @GetMapping
    public ResponseEntity<?> list(@AuthenticationPrincipal UserDetails userDetails) {
        User user = getUser(userDetails);
        List<SubscriptionResponse> result = subscriptionRepository.findByUser(user)
                .stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(result);
    }

    @PostMapping
    public ResponseEntity<?> create(@AuthenticationPrincipal UserDetails userDetails,
                                    @Valid @RequestBody SubscriptionRequest req) {
        User user = getUser(userDetails);

        Subscription sub = new Subscription();
        sub.setUser(user);
        sub.setMerchantName(req.merchantName());
        sub.setLogoUrl(req.logoUrl());
        sub.setAmount(req.amount());
        sub.setCurrency(req.currency() != null ? req.currency() : "USD");
        sub.setBillingCycle(req.billingCycle());
        sub.setNextBillingDate(req.nextBillingDate());
        sub.setStatus(req.status() != null ? req.status() : "ACTIVE");
        sub.setCategory(req.category());
        sub.setNotes(req.notes());
        sub.setIsTrial(req.isTrial() != null ? req.isTrial() : false);
        sub.setTrialEndDate(req.trialEndDate());
        sub.setCreatedAt(Instant.now());

        subscriptionRepository.save(sub);
        recordPriceHistory(sub);
        return ResponseEntity.ok(toResponse(sub));
    }

    /** Price history for the "how has this crept up over time" chart. */
    @GetMapping("/{id}/price-history")
    public ResponseEntity<?> priceHistory(@AuthenticationPrincipal UserDetails userDetails,
                                          @PathVariable UUID id) {
        User user = getUser(userDetails);
        Subscription sub = subscriptionRepository.findByIdAndUser(id, user).orElse(null);
        if (sub == null) {
            return ResponseEntity.status(404).body(Map.of("error", "Subscription not found"));
        }
        List<Map<String, Object>> history = priceHistoryRepository
                .findBySubscriptionOrderByEffectiveAtAsc(sub)
                .stream()
                .map(h -> Map.<String, Object>of(
                        "amount", h.getAmount(),
                        "currency", h.getCurrency(),
                        "effectiveAt", h.getEffectiveAt()))
                .toList();
        return ResponseEntity.ok(history);
    }

    private void recordPriceHistory(Subscription sub) {
        PriceHistory history = new PriceHistory();
        history.setSubscription(sub);
        history.setAmount(sub.getAmount());
        history.setCurrency(sub.getCurrency());
        history.setEffectiveAt(Instant.now());
        priceHistoryRepository.save(history);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@AuthenticationPrincipal UserDetails userDetails,
                                    @PathVariable UUID id,
                                    @Valid @RequestBody SubscriptionRequest req) {
        User user = getUser(userDetails);
        Subscription sub = subscriptionRepository.findByIdAndUser(id, user).orElse(null);

        if (sub == null) {
            return ResponseEntity.status(404).body(Map.of("error", "Subscription not found"));
        }

        BigDecimal previousAmount = sub.getAmount();

        sub.setMerchantName(req.merchantName());
        sub.setLogoUrl(req.logoUrl());
        sub.setAmount(req.amount());
        sub.setCurrency(req.currency() != null ? req.currency() : sub.getCurrency());
        sub.setBillingCycle(req.billingCycle());
        sub.setNextBillingDate(req.nextBillingDate());
        sub.setStatus(req.status() != null ? req.status() : sub.getStatus());
        sub.setCategory(req.category());
        sub.setNotes(req.notes());
        sub.setIsTrial(req.isTrial() != null ? req.isTrial() : sub.getIsTrial());
        sub.setTrialEndDate(req.trialEndDate());

        subscriptionRepository.save(sub);
        if (previousAmount == null || previousAmount.compareTo(sub.getAmount()) != 0) {
            recordPriceHistory(sub);
        }
        return ResponseEntity.ok(toResponse(sub));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@AuthenticationPrincipal UserDetails userDetails,
                                    @PathVariable UUID id) {
        User user = getUser(userDetails);
        Subscription sub = subscriptionRepository.findByIdAndUser(id, user).orElse(null);

        if (sub == null) {
            return ResponseEntity.status(404).body(Map.of("error", "Subscription not found"));
        }

        subscriptionRepository.delete(sub);
        return ResponseEntity.ok(Map.of("message", "Deleted"));
    }

    private User getUser(UserDetails userDetails) {
        return userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
    }

    private SubscriptionResponse toResponse(Subscription sub) {
        return new SubscriptionResponse(
                sub.getId(),
                sub.getMerchantName(),
                sub.getLogoUrl(),
                sub.getAmount(),
                sub.getCurrency(),
                sub.getBillingCycle(),
                sub.getNextBillingDate(),
                sub.getStatus(),
                sub.getCategory(),
                sub.getNotes(),
                sub.getIsTrial(),
                sub.getTrialEndDate(),
                sub.getCreatedAt()
        );
    }
}