package com.subscription_tracker.uchekd.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "subscriptions")
@Getter
@Setter
public class Subscription {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String merchantName;

    private String logoUrl;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private String currency = "USD";

    @Column(nullable = false)
    private String billingCycle; // WEEKLY, MONTHLY, QUARTERLY, ANNUAL

    @Column(nullable = false)
    private LocalDate nextBillingDate;

    @Column(nullable = false)
    private String status = "ACTIVE"; // ACTIVE, PAUSED, CANCELLED

    private String category;

    private String notes;

    @Column(nullable = false)
    private Boolean isTrial = false;

    private LocalDate trialEndDate;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}