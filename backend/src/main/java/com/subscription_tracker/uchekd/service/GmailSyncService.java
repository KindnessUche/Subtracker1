package com.subscription_tracker.uchekd.service;

import com.subscription_tracker.uchekd.dto.*;
import com.subscription_tracker.uchekd.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Service
public class GmailSyncService {

    @Autowired private GmailTokenService gmailTokenService;

    /** Widened beyond a subject-only match: `newer_than` uses the configured window so annual subs are caught. */
    @Value("${gmail.sync.window-days}")
    private int windowDays;

    /** Total messages to scan across pages; each one becomes a Gemini call downstream. */
    @Value("${gmail.sync.max-results}")
    private int maxResults;

    private final RestClient restClient = RestClient.create();

    /** Gmail caps a single list page at 500; we keep pages modest and paginate up to maxResults. */
    private static final int PAGE_SIZE = 100;

    /**
     * Widened from a narrow subject-only keyword list. Real billing emails frequently don't
     * contain any of the original keywords in the subject line (e.g. "Your Spotify Premium is
     * ready", "Thanks for your order") — so this now also matches on body text, not just
     * subject, and adds several more phrasings actually used by billing systems.
     */
    private static final String SUBJECT_KEYWORDS =
            "receipt OR invoice OR subscription OR payment OR billing OR renewal OR "
                    + "\"order confirmation\" OR \"payment confirmation\" OR \"your plan\" OR "
                    + "\"free trial\" OR \"trial ends\" OR \"trial expires\" OR membership OR "
                    + "\"auto-renew\" OR \"automatic renewal\" OR \"thank you for your purchase\"";

    private String buildQuery() {
        // Matching subject OR body (not subject-only) catches emails whose subject line is just
        // a brand/product name with the billing language only in the body.
        return "(subject:(" + SUBJECT_KEYWORDS + ") OR (" + SUBJECT_KEYWORDS + "))"
                + " newer_than:" + windowDays + "d";
    }

    public List<GmailMessageSummary> searchBillingEmails(User user) {
        String accessToken = gmailTokenService.getValidAccessToken(user);
        String query = buildQuery();

        List<GmailMessageRef> refs = new ArrayList<>();
        String pageToken = null;

        // Page through results until we hit maxResults or Gmail runs out of pages.
        do {
            final String currentPageToken = pageToken;
            final int pageSize = Math.min(PAGE_SIZE, maxResults - refs.size());

            GmailListMessagesResponse listResponse = restClient.get()
                    .uri(uriBuilder -> {
                        uriBuilder
                                .scheme("https").host("gmail.googleapis.com")
                                .path("/gmail/v1/users/me/messages")
                                .queryParam("q", query)
                                .queryParam("maxResults", pageSize);
                        if (currentPageToken != null) {
                            uriBuilder.queryParam("pageToken", currentPageToken);
                        }
                        return uriBuilder.build();
                    })
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .body(GmailListMessagesResponse.class);

            if (listResponse == null || listResponse.messages() == null) {
                break;
            }

            refs.addAll(listResponse.messages());
            pageToken = listResponse.nextPageToken();
        } while (pageToken != null && refs.size() < maxResults);

        // Hard-cap so the downstream Gemini calls can never exceed maxResults, even if a
        // page returned more than we asked for.
        if (refs.size() > maxResults) {
            refs = refs.subList(0, maxResults);
        }

        List<GmailMessageSummary> summaries = new ArrayList<>();
        for (GmailMessageRef ref : refs) {
            GmailMessageDetail detail = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("https").host("gmail.googleapis.com")
                            .path("/gmail/v1/users/me/messages/" + ref.id())
                            .queryParam("format", "metadata")
                            .queryParam("metadataHeaders", "Subject")
                            .queryParam("metadataHeaders", "From")
                            .queryParam("metadataHeaders", "Date")
                            .build())
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .body(GmailMessageDetail.class);

            if (detail != null) {
                summaries.add(toSummary(detail));
            }
        }

        return summaries;
    }

    public String getMessageBody(User user, String messageId) {
        String accessToken = gmailTokenService.getValidAccessToken(user);

        GmailFullMessage message = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .scheme("https").host("gmail.googleapis.com")
                        .path("/gmail/v1/users/me/messages/" + messageId)
                        .queryParam("format", "full")
                        .build())
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .body(GmailFullMessage.class);

        if (message == null || message.payload() == null) {
            return "";
        }

        String plainText = findPart(message.payload(), "text/plain");
        if (plainText != null) return plainText;

        String htmlText = findPart(message.payload(), "text/html");
        if (htmlText != null) return stripHtml(htmlText);

        return "";
    }

    private String findPart(GmailFullPayload payload, String mimeType) {
        if (mimeType.equals(payload.mimeType()) && payload.body() != null && payload.body().data() != null) {
            return decodeBase64Url(payload.body().data());
        }
        if (payload.parts() != null) {
            for (GmailFullPayload part : payload.parts()) {
                String found = findPart(part, mimeType);
                if (found != null) return found;
            }
        }
        return null;
    }

    private String decodeBase64Url(String data) {
        byte[] decoded = Base64.getUrlDecoder().decode(data);
        return new String(decoded, StandardCharsets.UTF_8);
    }

    private String stripHtml(String html) {
        return html.replaceAll("<[^>]+>", " ").replaceAll("&nbsp;", " ").replaceAll("\\s+", " ").trim();
    }

    private GmailMessageSummary toSummary(GmailMessageDetail detail) {
        return new GmailMessageSummary(
                detail.id(),
                header(detail, "Subject"),
                header(detail, "From"),
                header(detail, "Date"),
                detail.snippet()
        );
    }

    private String header(GmailMessageDetail detail, String name) {
        if (detail.payload() == null || detail.payload().headers() == null) return null;
        return detail.payload().headers().stream()
                .filter(h -> h.name().equalsIgnoreCase(name))
                .map(GmailHeader::value)
                .findFirst()
                .orElse(null);
    }
}