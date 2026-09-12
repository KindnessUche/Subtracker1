package com.subscription_tracker.uchekd.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * The default JWT secret and encryption key baked into application.properties exist ONLY so
 * `docker compose up` works out of the box for local development. Shipping either of them
 * unchanged to a real deployment would mean anyone who's read this open-source repo can forge
 * auth tokens or decrypt every stored Gmail OAuth token in the database.
 *
 * This runs once at boot and refuses to start if app.env=production is set but the secrets
 * are still the well-known defaults — fail loudly at startup, not silently in production.
 */
@Component
public class StartupSecurityGuard implements ApplicationRunner {

    private static final String DEFAULT_JWT_SECRET =
            "david_kindness_is_a_cracked_developer_change_me_in_production";
    private static final String DEFAULT_ENCRYPTION_KEY =
            "xhOec+hnnnQ/4QomO1eSxGqJRBhSE1murhiPIppsbkg=";

    @Value("${app.env}")
    private String appEnv;

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.encryption.key}")
    private String encryptionKey;

    @Override
    public void run(ApplicationArguments args) {
        if (!"production".equalsIgnoreCase(appEnv)) {
            return; // local/dev — the placeholder secrets are fine here
        }

        StringBuilder problems = new StringBuilder();
        if (DEFAULT_JWT_SECRET.equals(jwtSecret)) {
            problems.append("\n  - JWT_SECRET is still the repo's default value.");
        }
        if (DEFAULT_ENCRYPTION_KEY.equals(encryptionKey)) {
            problems.append("\n  - APP_ENCRYPTION_KEY is still the repo's default value.");
        }

        if (!problems.isEmpty()) {
            throw new IllegalStateException(
                    "Refusing to start with app.env=production while using default secrets:"
                            + problems
                            + "\nGenerate real values (e.g. `openssl rand -base64 32`) and set them "
                            + "via the JWT_SECRET / APP_ENCRYPTION_KEY environment variables.");
        }
    }
}
