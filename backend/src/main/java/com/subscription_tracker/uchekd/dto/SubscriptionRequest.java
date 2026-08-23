package com.subscription_tracker.uchekd.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SubscriptionRequest(
        @NotBlank String merchantName,
        String logoUrl,
        @NotNull @Positive BigDecimal amount,
        String currency,
        @NotBlank String billingCycle,
        @NotNull LocalDate nextBillingDate,
        String status,
        String category,
        String notes,
        Boolean isTrial,
        LocalDate trialEndDate
) {}