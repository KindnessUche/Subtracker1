package com.subscription_tracker.uchekd.service;

import com.subscription_tracker.uchekd.dto.GoogleTokenResponse;
import com.subscription_tracker.uchekd.model.GmailConnection;
import com.subscription_tracker.uchekd.model.User;
import com.subscription_tracker.uchekd.repository.GmailConnectionRepository;
import com.subscription_tracker.uchekd.security.TokenEncryptionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.time.Instant;

@Service
public class GmailTokenService {

    @Autowired private GmailConnectionRepository gmailConnectionRepository;
    @Autowired private TokenEncryptionService tokenEncryptionService;

    @Value("${google.client-id}")
    private String clientId;

    @Value("${google.client-secret}")
    private String clientSecret;

    private final RestClient restClient = RestClient.create();

    public String getValidAccessToken(User user) {
        GmailConnection connection = gmailConnectionRepository.findByUser(user)
                .orElseThrow(() -> new IllegalStateException("Gmail is not connected for this user"));

        if (connection.getExpiresAt().isAfter(Instant.now().plusSeconds(60))) {
            return tokenEncryptionService.decrypt(connection.getAccessToken());
        }

        String refreshToken = tokenEncryptionService.decrypt(connection.getRefreshToken());

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("refresh_token", refreshToken);
        body.add("grant_type", "refresh_token");

        GoogleTokenResponse response;
        try {
            response = restClient.post()
                    .uri("https://oauth2.googleapis.com/token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(body)
                    .retrieve()
                    .body(GoogleTokenResponse.class);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to refresh Gmail access token: " + e.getMessage(), e);
        }

        if (response == null || response.accessToken() == null) {
            throw new IllegalStateException("Google returned no access token on refresh");
        }

        connection.setAccessToken(tokenEncryptionService.encrypt(response.accessToken()));
        connection.setExpiresAt(Instant.now().plusSeconds(response.expiresIn()));
        gmailConnectionRepository.save(connection);

        return response.accessToken();
    }
}