package com.subscription_tracker.uchekd.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ReviewQueueItemResponse(
        UUID id,
        String type,
        String merchantName,
        BigDecimal amount,
        String currency,
        String billingCycle,
        BigDecimal previousAmount,
        Double confidenceScore,
        String rawSnippet,
        String status,
        Instant createdAt,
        Boolean isTrial,
        LocalDate trialEndDate
) {}