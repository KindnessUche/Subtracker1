package com.subscription_tracker.uchekd.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.subscription_tracker.uchekd.dto.ExtractedSubscription;
import com.subscription_tracker.uchekd.dto.GeminiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
public class GeminiExtractionService {

    private static final Logger log = LoggerFactory.getLogger(GeminiExtractionService.class);

    @Value("${google.gemini-api-key}")
    private String apiKey;

    @Value("${google.gemini-model}")
    private String model;

    /** Free-tier Gemini is rate-limited, so retry a few times on 429/5xx with exponential backoff. */
    private static final int MAX_ATTEMPTS = 4;

    /** Cap the prompt so a huge HTML email can't blow the token limit and produce truncated JSON. */
    private static final int MAX_EMAIL_CHARS = 12_000;

    private final RestClient restClient = RestClient.create();
    // This service builds its own ObjectMapper (rather than injecting Spring's autoconfigured
    // one) because it needs to parse Gemini's raw JSON text output directly, outside the normal
    // request/response pipeline. Registering JavaTimeModule explicitly is required here since a
    // bare `new ObjectMapper()` does NOT know how to parse the trialEndDate LocalDate field —
    // Spring's autoconfigured ObjectMapper gets that module for free, this one doesn't.
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private static final String PROMPT_TEMPLATE = """
            You are looking at a single email that may or may not be a subscription billing notice.
            Decide if it represents a recurring subscription charge — not a one-off purchase, not a bank statement, not spam.

            If a field cannot be determined: use null for numbers, "UNKNOWN" for billingCycle, false for booleans.
            isPriceChange should be true only if the email itself explicitly states or clearly implies the price increased from a stated previous amount.
            If isPriceChange is false, previousAmount MUST be null — do not populate it with any other number from the email.
            Do not infer a previousAmount from unrelated figures (fees, taxes, other line items) — only from an explicit "was X, now Y" statement.

            isTrial should be true only if the email explicitly describes an active free trial (e.g. "your free trial ends in 3 days",
            "trial period", "you won't be charged until"). If isTrial is true and the email states when the trial ends or when the
            first charge will occur, set trialEndDate to that date in YYYY-MM-DD format; otherwise leave it null.
            Do not set isTrial to true just because the product happens to offer trials in general — only if THIS email is about one.

            Email content:
            ---
            %s
            ---
            """;

    public ExtractedSubscription extract(String emailContent) {
        String trimmed = emailContent.length() > MAX_EMAIL_CHARS
                ? emailContent.substring(0, MAX_EMAIL_CHARS)
                : emailContent;

        Map<String, Object> schema = Map.of(
                "type", "OBJECT",
                "properties", Map.of(
                        "isSubscription", Map.of("type", "BOOLEAN"),
                        "merchantName", Map.of("type", "STRING"),
                        "amount", Map.of("type", "NUMBER"),
                        "currency", Map.of("type", "STRING"),
                        "billingCycle", Map.of("type", "STRING", "enum", List.of("WEEKLY", "MONTHLY", "QUARTERLY", "ANNUAL", "UNKNOWN")),
                        "isPriceChange", Map.of("type", "BOOLEAN"),
                        "previousAmount", Map.of("type", "NUMBER"),
                        "confidence", Map.of("type", "NUMBER"),
                        "isTrial", Map.of("type", "BOOLEAN"),
                        "trialEndDate", Map.of("type", "STRING")
                ),
                "required", List.of("isSubscription", "merchantName", "billingCycle", "isPriceChange", "confidence")
        );

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(Map.of("parts", List.of(Map.of("text", PROMPT_TEMPLATE.formatted(trimmed))))),
                "generationConfig", Map.of(
                        "responseMimeType", "application/json",
                        "responseSchema", schema
                )
        );

        GeminiResponse response = callGeminiWithRetry(requestBody);

        if (response == null || response.candidates() == null || response.candidates().isEmpty()) {
            throw new IllegalStateException("Gemini returned no candidates");
        }

        GeminiResponse.Candidate candidate = response.candidates().get(0);
        if (candidate.content() == null
                || candidate.content().parts() == null
                || candidate.content().parts().isEmpty()) {
            throw new IllegalStateException(
                    "Gemini returned an empty candidate (finishReason=" + candidate.finishReason() + ")");
        }

        String jsonText = candidate.content().parts().get(0).text();
        if (jsonText == null || jsonText.isBlank()) {
            throw new IllegalStateException(
                    "Gemini returned empty text (finishReason=" + candidate.finishReason() + ")");
        }

        try {
            return objectMapper.readValue(jsonText, ExtractedSubscription.class);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse Gemini's JSON output: " + e.getMessage(), e);
        }
    }

    private GeminiResponse callGeminiWithRetry(Map<String, Object> requestBody) {
        String url = "https://generativelanguage.googleapis.com/v1beta/models/"
                + model + ":generateContent?key=" + apiKey;

        int attempt = 0;
        while (true) {
            attempt++;
            try {
                return restClient.post()
                        .uri(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(requestBody)
                        .retrieve()
                        .body(GeminiResponse.class);
            } catch (HttpStatusCodeException e) {
                int status = e.getStatusCode().value();
                boolean retryable = status == 429 || status == 500 || status == 503;

                if (!retryable || attempt >= MAX_ATTEMPTS) {
                    // Surface the real reason (e.g. 404 = wrong model name, 400/403 = bad API key)
                    // instead of a generic failure, so callers can log something actionable.
                    throw new IllegalStateException(
                            "Gemini API call failed (HTTP " + status + ", model '" + model + "'): "
                                    + e.getResponseBodyAsString(), e);
                }

                long backoffMs = (long) Math.pow(2, attempt) * 500L; // 1s, 2s, 4s
                log.warn("Gemini returned HTTP {} (attempt {}/{}), backing off {}ms",
                        status, attempt, MAX_ATTEMPTS, backoffMs);
                try {
                    Thread.sleep(backoffMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Interrupted while backing off on Gemini retry", ie);
                }
            }
        }
    }
}
