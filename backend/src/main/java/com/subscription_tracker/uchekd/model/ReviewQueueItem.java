package com.subscription_tracker.uchekd.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "review_queue_items")
@Getter
@Setter
public class ReviewQueueItem {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String type; // NEW_SUBSCRIPTION or PRICE_CHANGE

    @Column(nullable = false)
    private String merchantName;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private String currency;

    @Column(nullable = false)
    private String billingCycle;

    @Column(precision = 12, scale = 2)
    private BigDecimal previousAmount;

    private Double confidenceScore;

    @Column(columnDefinition = "TEXT")
    private String rawSnippet;

    @Column(nullable = false)
    private String sourceMessageId;

    @Column(nullable = false)
    private String status = "PENDING"; // PENDING, APPROVED, DISMISSED

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}