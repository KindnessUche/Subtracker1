package com.subscription_tracker.uchekd.repository;

import com.subscription_tracker.uchekd.model.GmailConnection;
import com.subscription_tracker.uchekd.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface GmailConnectionRepository extends JpaRepository<GmailConnection, UUID> {
    Optional<GmailConnection> findByUser(User user);
}