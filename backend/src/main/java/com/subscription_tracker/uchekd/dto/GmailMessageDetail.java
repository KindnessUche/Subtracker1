package com.subscription_tracker.uchekd.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GmailMessageDetail(String id, String snippet, GmailPayload payload) {}