package com.subscription_tracker.uchekd.dto;

public record GmailMessageSummary(
        String id,
        String subject,
        String from,
        String date,
        String snippet
) {}