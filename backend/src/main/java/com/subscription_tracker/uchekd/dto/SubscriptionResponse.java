package com.subscription_tracker.uchekd.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record SubscriptionResponse(
        UUID id,
        String merchantName,
        String logoUrl,
        BigDecimal amount,
        String currency,
        String billingCycle,
        LocalDate nextBillingDate,
        String status,
        String category,
        String notes,
        Boolean isTrial,
        LocalDate trialEndDate,
        Instant createdAt
) {}