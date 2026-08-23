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
import java.util.function.Supplier;

/**
 * Instead of a filter that tries to set auth and hopes it sticks,
 * we tell Spring Security exactly how to LOAD the security context.
 * Spring Security calls this before any authorization check. No timing issues.
 */
@Component
public class JwtSecurityContextRepository implements SecurityContextRepository {

    @Autowired
    private JwtService jwtService;

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
        String header = request.getHeader("Authorization");

        System.out.println(">>> [JwtRepo] Request: " + request.getMethod() + " " + request.getRequestURI());
        System.out.println(">>> [JwtRepo] Authorization header: " + header);

        if (header == null || !header.startsWith("Bearer ")) {
            System.out.println(">>> [JwtRepo] No Bearer token — returning empty context");
            return context;
        }

        String token = header.substring(7).trim();

        if (jwtService.isTokenValid(token)) {
            String email = jwtService.extractEmail(token);
            System.out.println(">>> [JwtRepo] Token valid — setting auth for: " + email);

            UserDetails userDetails = User.withUsername(email)
                    .password("")
                    .authorities(Collections.emptyList())
                    .build();

            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());

            context.setAuthentication(auth);
        } else {
            System.out.println(">>> [JwtRepo] Token NOT valid — returning empty context");
        }

        return context;
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
        String header = request.getHeader("Authorization");
        return header != null && header.startsWith("Bearer ");
    }
}
