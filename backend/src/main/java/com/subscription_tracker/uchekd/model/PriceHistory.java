package com.subscription_tracker.uchekd.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * One row per price a subscription has ever had. Previously a price change overwrote
 * Subscription.amount directly with no record of what it used to be, so there was no way to
 * show "this went from $9.99 -> $12.99 -> $15.99 over the last year".
 */
@Entity
@Table(name = "price_history")
@Getter
@Setter
public class PriceHistory {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subscription_id", nullable = false)
    private Subscription subscription;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    /** When this price took effect (subscription creation, or the approved price-change date). */
    @Column(nullable = false, updatable = false)
    private Instant effectiveAt = Instant.now();
}
