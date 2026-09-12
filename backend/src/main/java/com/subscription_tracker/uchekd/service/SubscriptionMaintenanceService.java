package com.subscription_tracker.uchekd.service;

import com.subscription_tracker.uchekd.model.Subscription;
import com.subscription_tracker.uchekd.repository.SubscriptionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Runs daily. Handles the two things nothing in the codebase was previously doing:
 *
 *  1. Rolling nextBillingDate forward once it's in the past (previously it was set once at
 *     creation/approval time and never touched again, so it went permanently stale).
 *  2. Sending a reminder email a configurable number of days before a subscription renews.
 */
@Service
public class SubscriptionMaintenanceService {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionMaintenanceService.class);
    private static final String ACTIVE = "ACTIVE";

    @Autowired private SubscriptionRepository subscriptionRepository;
    @Autowired private BillingCycleService billingCycleService;
    @Autowired private NotificationService notificationService;

    /** How many days before renewal to send the reminder email. */
    @Value("${app.notifications.reminder-days-before:3}")
    private int reminderDaysBefore;

    /** Runs once a day at 07:00 server time. */
    @Scheduled(cron = "${app.scheduler.daily-maintenance-cron:0 0 7 * * *}")
    @Transactional
    public void runDailyMaintenance() {
        LocalDate today = LocalDate.now();
        int rolled = rollForwardOverdueDates(today);
        int notified = sendUpcomingRenewalReminders(today);
        log.info("Daily maintenance complete: {} subscription dates rolled forward, {} reminder emails sent",
                rolled, notified);
    }

    /**
     * Any ACTIVE subscription whose nextBillingDate is already in the past gets advanced,
     * possibly by more than one cycle if the app was down/unused for a while.
     */
    private int rollForwardOverdueDates(LocalDate today) {
        List<Subscription> overdue = subscriptionRepository.findByStatusAndNextBillingDateBefore(ACTIVE, today);
        for (Subscription sub : overdue) {
            LocalDate rolled = billingCycleService.rollForwardToFuture(
                    sub.getNextBillingDate(), sub.getBillingCycle(), today);
            sub.setNextBillingDate(rolled);
        }
        if (!overdue.isEmpty()) {
            subscriptionRepository.saveAll(overdue);
        }
        return overdue.size();
    }

    /**
     * Sends one reminder per subscription per renewal date. Uses lastReminderSentFor to avoid
     * re-sending every day the job runs while the subscription sits inside the reminder window.
     */
    private int sendUpcomingRenewalReminders(LocalDate today) {
        LocalDate cutoff = today.plusDays(reminderDaysBefore);
        List<Subscription> candidates = subscriptionRepository
                .findByStatusAndNextBillingDateLessThanEqual(ACTIVE, cutoff);

        int sent = 0;
        for (Subscription sub : candidates) {
            boolean alreadyNotifiedForThisDate = sub.getNextBillingDate().equals(sub.getLastReminderSentFor());
            if (alreadyNotifiedForThisDate) {
                continue;
            }
            boolean success = notificationService.sendRenewalReminder(sub);
            if (success) {
                sub.setLastReminderSentFor(sub.getNextBillingDate());
                subscriptionRepository.save(sub);
                sent++;
            }
        }
        return sent;
    }
}
