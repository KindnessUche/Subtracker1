package com.subscription_tracker.uchekd.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.time.LocalDate;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ExtractedSubscription(
        boolean isSubscription,
        String merchantName,
        BigDecimal amount,
        String currency,
        String billingCycle,
        boolean isPriceChange,
        BigDecimal previousAmount,
        double confidence,
        boolean isTrial,
        LocalDate trialEndDate
) {}