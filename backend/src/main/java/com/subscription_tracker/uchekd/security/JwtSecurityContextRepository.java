package com.subscription_tracker.uchekd.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.DeferredSecurityContext;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.context.HttpRequestResponseHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Component;

import java.util.Collections;

/**
 * Instead of a filter that tries to set auth and hopes it sticks,
 * we tell Spring Security exactly how to LOAD the security context.
 * Spring Security calls this before any authorization check. No timing issues.
 *
 * The JWT is read primarily from the httpOnly auth cookie (set by AuthController on
 * login/signup). The Authorization: Bearer header is still honored as a fallback so
 * non-browser API clients (scripts, Postman, mobile) can keep working without cookies.
 */
@Component
public class JwtSecurityContextRepository implements SecurityContextRepository {

    @Autowired
    private JwtService jwtService;

    @Autowired
    private AuthCookieService authCookieService;

    @Override
    public DeferredSecurityContext loadDeferredContext(HttpServletRequest request) {
        return new DeferredSecurityContext() {
            private SecurityContext context;

            @Override
            public SecurityContext get() {
                if (context == null) {
                    context = buildContext(request);
                }
                return context;
            }

            @Override
            public boolean isGenerated() {
                return context != null;
            }
        };
    }

    private SecurityContext buildContext(HttpServletRequest request) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();

        String token = extractToken(request);
        if (token == null) {
            return context;
        }

        if (jwtService.isTokenValid(token)) {
            String email = jwtService.extractEmail(token);

            UserDetails userDetails = User.withUsername(email)
                    .password("")
                    .authorities(Collections.emptyList())
                    .build();

            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());

            context.setAuthentication(auth);
        }

        return context;
    }

    private String extractToken(HttpServletRequest request) {
        String cookieToken = authCookieService.readToken(request);
        if (cookieToken != null) {
            return cookieToken;
        }

        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7).trim();
        }

        return null;
    }

    @Override
    @SuppressWarnings("deprecation")
    public SecurityContext loadContext(HttpRequestResponseHolder requestResponseHolder) {
        return buildContext(requestResponseHolder.getRequest());
    }

    @Override
    public void saveContext(SecurityContext context, HttpServletRequest request, HttpServletResponse response) {
        // Stateless — never save to session
    }

    @Override
    public boolean containsContext(HttpServletRequest request) {
        return extractToken(request) != null;
    }
}
