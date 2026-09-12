package com.subscription_tracker.uchekd.service;

import com.subscription_tracker.uchekd.model.Subscription;
import com.subscription_tracker.uchekd.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class NotificationServiceTest {

    private JavaMailSender mailSender;
    private NotificationService service;

    @BeforeEach
    void setUp() {
        mailSender = mock(JavaMailSender.class);
        service = new NotificationService(mailSender);
        ReflectionTestUtils.setField(service, "fromAddress", "no-reply@subtracker.local");
        ReflectionTestUtils.setField(service, "mailEnabled", true);
    }

    private Subscription sampleSubscription() {
        User user = new User();
        user.setEmail("user@example.com");

        Subscription sub = new Subscription();
        sub.setUser(user);
        sub.setMerchantName("Netflix");
        sub.setAmount(new BigDecimal("15.99"));
        sub.setCurrency("USD");
        sub.setBillingCycle("MONTHLY");
        sub.setNextBillingDate(LocalDate.of(2026, 10, 1));
        return sub;
    }

    @Test
    void sendRenewalReminder_whenMailEnabled_sendsToSubscriptionOwner() {
        boolean result = service.sendRenewalReminder(sampleSubscription());

        assertTrue(result);
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        assertEquals("user@example.com", captor.getValue().getTo()[0]);
        assertTrue(captor.getValue().getSubject().contains("Netflix"));
    }

    @Test
    void sendRenewalReminder_whenMailDisabled_doesNotCallMailSender() {
        ReflectionTestUtils.setField(service, "mailEnabled", false);

        boolean result = service.sendRenewalReminder(sampleSubscription());

        assertFalse(result);
        verifyNoInteractions(mailSender);
    }

    @Test
    void sendRenewalReminder_whenMailSenderThrows_returnsFalseInsteadOfPropagating() {
        doThrow(new RuntimeException("SMTP down")).when(mailSender).send(any(SimpleMailMessage.class));

        // A bad send must not throw — the scheduled job sends many of these in a loop and
        // one broken address/SMTP outage should not stop reminders for everyone else.
        boolean result = service.sendRenewalReminder(sampleSubscription());

        assertFalse(result);
    }

    @Test
    void sendPriceChangeAlert_mentionsBothOldAndNewAmount() {
        Subscription sub = sampleSubscription();
        sub.setAmount(new BigDecimal("17.99"));

        service.sendPriceChangeAlert(sub, new BigDecimal("15.99"));

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        String body = captor.getValue().getText();
        assertNotNull(body);
        assertTrue(body.contains("15.99"));
        assertTrue(body.contains("17.99"));
    }
}
