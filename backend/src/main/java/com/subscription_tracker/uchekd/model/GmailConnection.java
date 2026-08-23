package com.subscription_tracker.uchekd.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "gmail_connections")
@Getter
@Setter
public class GmailConnection {

    @Id
    @GeneratedValue
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String accessToken;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String refreshToken;

    @Column(nullable = false)
    private Instant expiresAt;

    private Instant lastSyncAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}