package com.subscription_tracker.uchekd.service;

import org.springframework.stereotype.Service;

import java.time.LocalDate;

/**
 * Single source of truth for "how far apart are two billing dates" math.
 * Previously this logic was duplicated (and only ever applied once, at
 * creation time) inside ReviewQueueController — nothing ever advanced a
 * date after that, so nextBillingDate went stale as soon as it passed.
 */
@Service
public class BillingCycleService {

    public LocalDate firstBillingDate(String billingCycle) {
        return advance(LocalDate.now(), billingCycle);
    }

    public LocalDate advance(LocalDate from, String billingCycle) {
        if (billingCycle == null) {
            return from.plusMonths(1);
        }
        return switch (billingCycle) {
            case "WEEKLY" -> from.plusWeeks(1);
            case "QUARTERLY" -> from.plusMonths(3);
            case "ANNUAL" -> from.plusYears(1);
            case "MONTHLY" -> from.plusMonths(1);
            default -> from.plusMonths(1); // UNKNOWN — best-effort default
        };
    }

    /**
     * Roll a date forward by its billing cycle repeatedly until it is no longer in the past.
     * Handles subscriptions that have been overdue for multiple cycles (e.g. the app wasn't
     * running for a while) instead of only advancing by one period.
     */
    public LocalDate rollForwardToFuture(LocalDate nextBillingDate, String billingCycle, LocalDate today) {
        LocalDate result = nextBillingDate;
        int guard = 0; // safety net in case of a misconfigured cycle that doesn't advance
        while (result.isBefore(today) && guard < 1000) {
            result = advance(result, billingCycle);
            guard++;
        }
        return result;
    }
}
