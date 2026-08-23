package com.subscription_tracker.uchekd.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GmailFullPayload(String mimeType, GmailBody body, List<GmailFullPayload> parts) {}