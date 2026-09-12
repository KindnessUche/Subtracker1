package com.subscription_tracker.uchekd.service;

import com.subscription_tracker.uchekd.model.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

/**
 * Sends renewal-reminder emails. Wraps JavaMailSender so the scheduler doesn't
 * need to know about mail internals, and so one bad send doesn't need to be
 * handled differently from any other.
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MMM d, yyyy");

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String fromAddress;

    @Value("${app.mail.enabled:true}")
    private boolean mailEnabled;

    public NotificationService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /** Send a "this subscription renews soon" reminder. Returns true if the send succeeded. */
    public boolean sendRenewalReminder(Subscription sub) {
        String to = sub.getUser().getEmail();
        String subject = "Renewing soon: " + sub.getMerchantName() + " on " + sub.getNextBillingDate().format(DATE_FORMAT);
        String body = buildRenewalBody(sub);
        return send(to, subject, body);
    }

    /** Send a "the price for this subscription changed" alert, distinct from a plain renewal reminder. */
    public boolean sendPriceChangeAlert(Subscription sub, BigDecimal previousAmount) {
        String to = sub.getUser().getEmail();
        String subject = "Price change: " + sub.getMerchantName();
        String body = "Heads up — " + sub.getMerchantName() + " changed from "
                + sub.getCurrency() + " " + previousAmount + " to "
                + sub.getCurrency() + " " + sub.getAmount() + " per " + cycleLabel(sub.getBillingCycle()) + ".";
        return send(to, subject, body);
    }

    private String buildRenewalBody(Subscription sub) {
        return "Reminder: " + sub.getMerchantName() + " ("
                + sub.getCurrency() + " " + sub.getAmount() + " / " + cycleLabel(sub.getBillingCycle()) + ")"
                + " renews on " + sub.getNextBillingDate().format(DATE_FORMAT) + ".\n\n"
                + "If you don't need it anymore, cancel it before the renewal date.\n\n"
                + "— Subtracker";
    }

    private String cycleLabel(String billingCycle) {
        if (billingCycle == null) return "cycle";
        return switch (billingCycle) {
            case "WEEKLY" -> "week";
            case "QUARTERLY" -> "quarter";
            case "ANNUAL" -> "year";
            case "MONTHLY" -> "month";
            default -> billingCycle.toLowerCase();
        };
    }

    private boolean send(String to, String subject, String body) {
        if (!mailEnabled) {
            log.info("Mail disabled (app.mail.enabled=false) — skipping email to {} [{}]", to, subject);
            return false;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            return true;
        } catch (Exception e) {
            // A single failed send (bad SMTP creds, transient network error) should never
            // take down the scheduled job for every other user's reminders.
            log.warn("Failed to send email to {}: {}", to, e.getMessage());
            return false;
        }
    }
}
