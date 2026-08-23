package com.subscription_tracker.uchekd.controller;

import com.subscription_tracker.uchekd.dto.ExtractedSubscription;
import com.subscription_tracker.uchekd.dto.GmailMessageSummary;
import com.subscription_tracker.uchekd.dto.GoogleTokenResponse;
import com.subscription_tracker.uchekd.model.GmailConnection;
import com.subscription_tracker.uchekd.model.User;
import com.subscription_tracker.uchekd.repository.GmailConnectionRepository;
import com.subscription_tracker.uchekd.repository.UserRepository;
import com.subscription_tracker.uchekd.security.OAuthStateStore;
import com.subscription_tracker.uchekd.security.TokenEncryptionService;
import com.subscription_tracker.uchekd.service.GeminiExtractionService;
import com.subscription_tracker.uchekd.service.GmailSyncService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/gmail")
public class GmailController {

    @Autowired private UserRepository userRepository;
    @Autowired private GmailConnectionRepository gmailConnectionRepository;
    @Autowired private TokenEncryptionService tokenEncryptionService;
    @Autowired private OAuthStateStore oAuthStateStore;
    @Autowired private GmailSyncService gmailSyncService;
    @Autowired private GeminiExtractionService geminiExtractionService;

    @Value("${google.client-id}")
    private String clientId;

    @Value("${google.client-secret}")
    private String clientSecret;

    @Value("${google.redirect-uri}")
    private String redirectUri;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    private final RestClient restClient = RestClient.create();

    @GetMapping("/connect")
    public ResponseEntity<?> connect(@AuthenticationPrincipal UserDetails userDetails) {
        String state = oAuthStateStore.create(userDetails.getUsername());

        String authUrl = UriComponentsBuilder
                .fromHttpUrl("https://accounts.google.com/o/oauth2/v2/auth")
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("response_type", "code")
                .queryParam("scope", "https://www.googleapis.com/auth/gmail.readonly")
                .queryParam("access_type", "offline")
                .queryParam("prompt", "consent")
                .queryParam("state", state)
                .build()
                .toUriString();

        return ResponseEntity.ok(Map.of("authUrl", authUrl));
    }

    @GetMapping("/callback")
    public ResponseEntity<?> callback(@RequestParam("code") String code,
                                      @RequestParam("state") String state) {
        String userEmail = oAuthStateStore.consume(state);
        if (userEmail == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Invalid or expired connection attempt. Please try again."));
        }

        User user = userRepository.findByEmail(userEmail).orElseThrow();

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("code", code);
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("redirect_uri", redirectUri);
        body.add("grant_type", "authorization_code");

        GoogleTokenResponse tokenResponse;
        try {
            tokenResponse = restClient.post()
                    .uri("https://oauth2.googleapis.com/token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(body)
                    .retrieve()
                    .body(GoogleTokenResponse.class);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(Map.of("error", "Google rejected the token exchange: " + e.getMessage()));
        }

        if (tokenResponse == null || tokenResponse.accessToken() == null) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(Map.of("error", "Failed to exchange code with Google"));
        }

        GmailConnection connection = gmailConnectionRepository.findByUser(user).orElseGet(() -> {
            GmailConnection c = new GmailConnection();
            c.setUser(user);
            c.setCreatedAt(Instant.now());
            return c;
        });

        connection.setAccessToken(tokenEncryptionService.encrypt(tokenResponse.accessToken()));

        if (tokenResponse.refreshToken() != null) {
            connection.setRefreshToken(tokenEncryptionService.encrypt(tokenResponse.refreshToken()));
        } else if (connection.getRefreshToken() == null) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(Map.of("error", "Google didn't return a refresh token. Remove Uchekd's access at myaccount.google.com/permissions and try connecting again."));
        }

        connection.setExpiresAt(Instant.now().plusSeconds(tokenResponse.expiresIn()));
        gmailConnectionRepository.save(connection);

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(frontendUrl + "/dashboard?gmail=connected"))
                .build();
    }

    @GetMapping("/status")
    public ResponseEntity<?> status(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        boolean connected = gmailConnectionRepository.findByUser(user).isPresent();
        return ResponseEntity.ok(Map.of("connected", connected));
    }

    @GetMapping("/sync")
    public ResponseEntity<?> sync(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        try {
            List<GmailMessageSummary> results = gmailSyncService.searchBillingEmails(user);
            return ResponseEntity.ok(Map.of("count", results.size(), "emails", results));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of("error", "Gmail sync failed: " + e.getMessage()));
        }
    }

    @GetMapping("/extract/{messageId}")
    public ResponseEntity<?> extract(@AuthenticationPrincipal UserDetails userDetails, @PathVariable String messageId) {
        User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        try {
            String body = gmailSyncService.getMessageBody(user, messageId);
            if (body.isBlank()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Could not extract readable content from this email"));
            }
            ExtractedSubscription result = geminiExtractionService.extract(body);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of("error", "Extraction failed: " + e.getMessage()));
        }
    }
}