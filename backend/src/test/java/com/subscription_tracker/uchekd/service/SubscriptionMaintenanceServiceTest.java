package com.subscription_tracker.uchekd.service;

import com.subscription_tracker.uchekd.model.Subscription;
import com.subscription_tracker.uchekd.repository.SubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class SubscriptionMaintenanceServiceTest {

    private SubscriptionRepository subscriptionRepository;
    private BillingCycleService billingCycleService;
    private NotificationService notificationService;
    private SubscriptionMaintenanceService maintenanceService;

    @BeforeEach
    void setUp() {
        subscriptionRepository = mock(SubscriptionRepository.class);
        billingCycleService = new BillingCycleService(); // real implementation, it's pure logic
        notificationService = mock(NotificationService.class);

        maintenanceService = new SubscriptionMaintenanceService();
        ReflectionTestUtils.setField(maintenanceService, "subscriptionRepository", subscriptionRepository);
        ReflectionTestUtils.setField(maintenanceService, "billingCycleService", billingCycleService);
        ReflectionTestUtils.setField(maintenanceService, "notificationService", notificationService);
        ReflectionTestUtils.setField(maintenanceService, "reminderDaysBefore", 3);
    }

    private Subscription subscriptionDueOn(LocalDate date, String cycle) {
        Subscription sub = new Subscription();
        sub.setMerchantName("Netflix");
        sub.setStatus("ACTIVE");
        sub.setBillingCycle(cycle);
        sub.setNextBillingDate(date);
        return sub;
    }

    @Test
    void runDailyMaintenance_rollsForwardOverdueSubscription() {
        Subscription overdue = subscriptionDueOn(LocalDate.of(2026, 1, 1), "MONTHLY");
        when(subscriptionRepository.findByStatusAndNextBillingDateBefore(eq("ACTIVE"), any()))
                .thenReturn(List.of(overdue));
        when(subscriptionRepository.findByStatusAndNextBillingDateLessThanEqual(eq("ACTIVE"), any()))
                .thenReturn(List.of());

        maintenanceService.runDailyMaintenance();

        // Overdue Jan 1 subscription should have been rolled forward to a date no longer in the past.
        assertEquals(false, overdue.getNextBillingDate().isBefore(LocalDate.now()));
        verify(subscriptionRepository).saveAll(List.of(overdue));
    }

    @Test
    void runDailyMaintenance_sendsReminderOnceForUpcomingRenewal() {
        Subscription upcoming = subscriptionDueOn(LocalDate.now().plusDays(2), "MONTHLY");
        when(subscriptionRepository.findByStatusAndNextBillingDateBefore(eq("ACTIVE"), any()))
                .thenReturn(List.of());
        when(subscriptionRepository.findByStatusAndNextBillingDateLessThanEqual(eq("ACTIVE"), any()))
                .thenReturn(List.of(upcoming));
        when(notificationService.sendRenewalReminder(upcoming)).thenReturn(true);

        maintenanceService.runDailyMaintenance();

        verify(notificationService, times(1)).sendRenewalReminder(upcoming);
        assertEquals(upcoming.getNextBillingDate(), upcoming.getLastReminderSentFor());
    }

    @Test
    void runDailyMaintenance_doesNotDoubleNotify_whenAlreadyRemindedForThisDate() {
        LocalDate renewalDate = LocalDate.now().plusDays(1);
        Subscription alreadyNotified = subscriptionDueOn(renewalDate, "MONTHLY");
        alreadyNotified.setLastReminderSentFor(renewalDate); // simulates yesterday's run already sent this

        when(subscriptionRepository.findByStatusAndNextBillingDateBefore(eq("ACTIVE"), any()))
                .thenReturn(List.of());
        when(subscriptionRepository.findByStatusAndNextBillingDateLessThanEqual(eq("ACTIVE"), any()))
                .thenReturn(List.of(alreadyNotified));

        maintenanceService.runDailyMaintenance();

        verify(notificationService, never()).sendRenewalReminder(any());
    }
}
