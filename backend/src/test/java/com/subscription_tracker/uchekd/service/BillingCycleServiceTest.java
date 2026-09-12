package com.subscription_tracker.uchekd.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BillingCycleServiceTest {

    private final BillingCycleService service = new BillingCycleService();

    @Test
    void advance_monthly_addsOneMonth() {
        LocalDate result = service.advance(LocalDate.of(2026, 1, 15), "MONTHLY");
        assertEquals(LocalDate.of(2026, 2, 15), result);
    }

    @Test
    void advance_weekly_addsSevenDays() {
        LocalDate result = service.advance(LocalDate.of(2026, 1, 1), "WEEKLY");
        assertEquals(LocalDate.of(2026, 1, 8), result);
    }

    @Test
    void advance_quarterly_addsThreeMonths() {
        LocalDate result = service.advance(LocalDate.of(2026, 1, 1), "QUARTERLY");
        assertEquals(LocalDate.of(2026, 4, 1), result);
    }

    @Test
    void advance_annual_addsOneYear() {
        LocalDate result = service.advance(LocalDate.of(2026, 1, 1), "ANNUAL");
        assertEquals(LocalDate.of(2027, 1, 1), result);
    }

    @Test
    void advance_unknownOrNullCycle_defaultsToMonthly() {
        assertEquals(LocalDate.of(2026, 2, 1), service.advance(LocalDate.of(2026, 1, 1), "UNKNOWN"));
        assertEquals(LocalDate.of(2026, 2, 1), service.advance(LocalDate.of(2026, 1, 1), null));
    }

    @Test
    void rollForwardToFuture_alreadyInFuture_returnsUnchanged() {
        LocalDate future = LocalDate.of(2099, 1, 1);
        LocalDate result = service.rollForwardToFuture(future, "MONTHLY", LocalDate.of(2026, 1, 1));
        assertEquals(future, result);
    }

    @Test
    void rollForwardToFuture_onePastCycle_advancesOnce() {
        // Billing date was Jan 1, today is Jan 15 -> next charge should be Feb 1.
        LocalDate result = service.rollForwardToFuture(
                LocalDate.of(2026, 1, 1), "MONTHLY", LocalDate.of(2026, 1, 15));
        assertEquals(LocalDate.of(2026, 2, 1), result);
    }

    @Test
    void rollForwardToFuture_manyMissedCycles_catchesAllTheWayUp() {
        // Weekly subscription whose date hasn't been touched in ~10 weeks — this is exactly the
        // "app wasn't running for a while" case that a single +1 advance would get wrong.
        LocalDate staleDate = LocalDate.of(2026, 1, 1);
        LocalDate today = LocalDate.of(2026, 3, 15); // ~10 weeks later
        LocalDate result = service.rollForwardToFuture(staleDate, "WEEKLY", today);

        assertEquals(true, !result.isBefore(today), "result must not still be in the past");
        // And it should still land on a date that is exactly N weeks after the original,
        // not just "today" — i.e. it preserves the billing cadence/anchor day.
        long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(staleDate, result);
        assertEquals(0, daysBetween % 7, "rolled-forward date should still fall on the original weekly cadence");
    }
}
