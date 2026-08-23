package com.subscription_tracker.uchekd.controller;

import com.subscription_tracker.uchekd.dto.AuthResponse;
import com.subscription_tracker.uchekd.dto.LoginRequest;
import com.subscription_tracker.uchekd.dto.SignupRequest;
import com.subscription_tracker.uchekd.model.User;
import com.subscription_tracker.uchekd.repository.UserRepository;
import com.subscription_tracker.uchekd.security.JwtService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
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

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@Valid @RequestBody SignupRequest req) {
        if (userRepository.existsByEmail(req.email())) {
            return ResponseEntity.status(409).body(Map.of("error", "Email already in use"));
        }
        User user = new User();
        user.setEmail(req.email());
        user.setPasswordHash(passwordEncoder.encode(req.password()));
        userRepository.save(user);
        return ResponseEntity.ok(new AuthResponse(jwtService.generateToken(user.getEmail())));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest req) {
        User user = userRepository.findByEmail(req.email()).orElse(null);
        if (user == null || !passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid email or password"));
        }
        return ResponseEntity.ok(new AuthResponse(jwtService.generateToken(user.getEmail())));
    }
}
