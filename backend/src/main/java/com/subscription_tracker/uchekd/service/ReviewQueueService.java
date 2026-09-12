package com.subscription_tracker.uchekd.service;

import com.subscription_tracker.uchekd.dto.ExtractedSubscription;
import com.subscription_tracker.uchekd.dto.GmailMessageSummary;
import com.subscription_tracker.uchekd.model.ReviewQueueItem;
import com.subscription_tracker.uchekd.model.User;
import com.subscription_tracker.uchekd.repository.ReviewQueueItemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Service
public class ReviewQueueService {

    private static final Logger log = LoggerFactory.getLogger(ReviewQueueService.class);

    @Autowired private GmailSyncService gmailSyncService;
    @Autowired private GeminiExtractionService geminiExtractionService;
    @Autowired private ReviewQueueItemRepository reviewQueueItemRepository;

    private static final double MIN_CONFIDENCE = 0.5;

    /** Result of a sync so the controller can tell "found nothing" apart from "everything errored". */
    public record SyncResult(int scanned, int queued, int skipped, int failed) {}

    public SyncResult syncAndQueue(User user) {
        List<GmailMessageSummary> candidates = gmailSyncService.searchBillingEmails(user);
        int queued = 0;
        int skipped = 0;
        int failed = 0;

        for (GmailMessageSummary candidate : candidates) {
            if (reviewQueueItemRepository.existsByUserAndSourceMessageId(user, candidate.id())) {
                skipped++;
                continue;
            }

            String body;
            try {
                body = gmailSyncService.getMessageBody(user, candidate.id());
            } catch (Exception e) {
                // Previously swallowed silently — now counted and logged so a systemic
                // failure (e.g. token/scope problem) is visible instead of looking like "no subs".
                log.warn("Failed to fetch body for message {}: {}", candidate.id(), e.getMessage());
                failed++;
                continue;
            }
            if (body.isBlank()) {
                skipped++;
                continue;
            }

            ExtractedSubscription extracted;
            try {
                extracted = geminiExtractionService.extract(body);
            } catch (Exception e) {
                // A wrong model name or bad API key lands here for EVERY email; logging it is
                // what makes that diagnosable instead of the pipeline silently queueing nothing.
                log.warn("Gemini extraction failed for message {}: {}", candidate.id(), e.getMessage());
                failed++;
                continue;
            }

            if (!extracted.isSubscription() || extracted.confidence() < MIN_CONFIDENCE) {
                skipped++;
                continue;
            }

            // A subscription with no amount is not trustworthy data — leave it for review as null
            // rather than inventing $0.00, which would silently drag down spend totals.
            if (extracted.amount() == null && !extracted.isPriceChange()) {
                log.info("Skipping '{}' — flagged as subscription but no amount could be parsed",
                        extracted.merchantName());
                skipped++;
                continue;
            }

            ReviewQueueItem item = new ReviewQueueItem();
            item.setUser(user);
            item.setType(extracted.isPriceChange() ? "PRICE_CHANGE" : "NEW_SUBSCRIPTION");
            item.setMerchantName(extracted.merchantName() != null ? extracted.merchantName() : "Unknown");
            item.setAmount(extracted.amount() != null ? extracted.amount() : BigDecimal.ZERO);
            item.setCurrency(extracted.currency() != null && !extracted.currency().isBlank() ? extracted.currency() : "USD");
            item.setBillingCycle(extracted.billingCycle() != null ? extracted.billingCycle() : "UNKNOWN");
            item.setPreviousAmount(extracted.isPriceChange() ? extracted.previousAmount() : null);
            item.setIsTrial(extracted.isTrial());
            item.setTrialEndDate(extracted.isTrial() ? extracted.trialEndDate() : null);
            item.setConfidenceScore(extracted.confidence());
            item.setRawSnippet(candidate.snippet());
            item.setSourceMessageId(candidate.id());
            item.setStatus("PENDING");
            item.setCreatedAt(Instant.now());

            reviewQueueItemRepository.save(item);
            queued++;
        }

        SyncResult result = new SyncResult(candidates.size(), queued, skipped, failed);
        log.info("Sync for {} complete: {}", user.getEmail(), result);

        // If we scanned emails but EVERY one errored, this is a real failure (bad model/key/token),
        // not an empty inbox — surface it so the user isn't told "0 found" on a broken pipeline.
        if (!candidates.isEmpty() && failed == candidates.size()) {
            throw new IllegalStateException(
                    "All " + failed + " candidate emails failed to process. "
                            + "This usually means the Gemini model name or API key is wrong, "
                            + "or the Gmail token lost its scope. Check server logs for details.");
        }

        return result;
    }
}
