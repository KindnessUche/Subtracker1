package com.subscription_tracker.uchekd.controller;

import com.subscription_tracker.uchekd.dto.LoginRequest;
import com.subscription_tracker.uchekd.dto.SignupRequest;
import com.subscription_tracker.uchekd.model.User;
import com.subscription_tracker.uchekd.repository.UserRepository;
import com.subscription_tracker.uchekd.security.AuthCookieService;
import com.subscription_tracker.uchekd.security.JwtService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private AuthCookieService authCookieService;

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@Valid @RequestBody SignupRequest req, HttpServletResponse response) {
        if (userRepository.existsByEmail(req.email())) {
            return ResponseEntity.status(409).body(Map.of("error", "Email already in use"));
        }
        User user = new User();
        user.setEmail(req.email());
        user.setPasswordHash(passwordEncoder.encode(req.password()));
        userRepository.save(user);

        String token = jwtService.generateToken(user.getEmail());
        authCookieService.setAuthCookie(response, token);
        // The token itself is intentionally NOT returned in the body anymore — it lives only in
        // the httpOnly cookie so client-side JS (and any XSS payload) never has access to it.
        return ResponseEntity.ok(Map.of("email", user.getEmail()));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest req, HttpServletResponse response) {
        User user = userRepository.findByEmail(req.email()).orElse(null);
        if (user == null || !passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid email or password"));
        }

        String token = jwtService.generateToken(user.getEmail());
        authCookieService.setAuthCookie(response, token);
        return ResponseEntity.ok(Map.of("email", user.getEmail()));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse response) {
        authCookieService.clearAuthCookie(response);
        return ResponseEntity.ok(Map.of("status", "logged out"));
    }

    /** Lets the frontend check "am I logged in" without ever touching the token itself. */
    @GetMapping("/me")
    public ResponseEntity<?> me(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }
        return ResponseEntity.ok(Map.of("email", userDetails.getUsername()));
    }
}
