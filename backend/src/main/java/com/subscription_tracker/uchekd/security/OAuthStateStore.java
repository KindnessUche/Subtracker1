package com.subscription_tracker.uchekd.security;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class OAuthStateStore {

    private record PendingState(String userEmail, Instant expiresAt) {}

    private final Map<String, PendingState> states = new ConcurrentHashMap<>();

    public String create(String userEmail) {
        String state = UUID.randomUUID().toString();
        states.put(state, new PendingState(userEmail, Instant.now().plusSeconds(600)));
        return state;
    }

    public String consume(String state) {
        PendingState pending = states.remove(state);
        if (pending == null || pending.expiresAt().isBefore(Instant.now())) {
            return null;
        }
        return pending.userEmail();
    }
}