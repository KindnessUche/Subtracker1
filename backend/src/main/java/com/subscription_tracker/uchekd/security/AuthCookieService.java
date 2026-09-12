package com.subscription_tracker.uchekd.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/**
 * Centralizes the auth cookie so it's set/read/cleared the same way everywhere. Using an
 * httpOnly cookie instead of returning the JWT in the response body for the frontend to put in
 * localStorage means a successful XSS on the frontend can no longer just read the token out of
 * storage and exfiltrate it — the browser withholds document.cookie access for httpOnly cookies.
 *
 * Uses Spring's ResponseCookie (rather than jakarta.servlet.http.Cookie) so we can set
 * SameSite explicitly, which the raw servlet Cookie API doesn't expose.
 */
@Component
public class AuthCookieService {

    public static final String COOKIE_NAME = "uchekd_token";

    @Value("${app.jwt.expiration-ms}")
    private long expirationMs;

    @Value("${app.cookie.secure:true}")
    private boolean secureCookie;

    public void setAuthCookie(HttpServletResponse response, String token) {
        ResponseCookie cookie = ResponseCookie.from(COOKIE_NAME, token)
                .httpOnly(true)
                .secure(secureCookie)
                .path("/")
                .sameSite("Lax")
                .maxAge(expirationMs / 1000)
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }

    public void clearAuthCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(COOKIE_NAME, "")
                .httpOnly(true)
                .secure(secureCookie)
                .path("/")
                .sameSite("Lax")
                .maxAge(0)
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }

    public String readToken(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return null;
        }
        for (var cookie : request.getCookies()) {
            if (COOKIE_NAME.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
