package com.qrapp.controller;

import java.util.Map;

import com.qrapp.entity.User;
import com.qrapp.repository.UserRepository;
import com.qrapp.service.AuthService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
// ...existing code...
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
// ...existing code...
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {
    @Autowired
    private com.qrapp.service.SuspiciousActivityService suspiciousActivityService;
    @Autowired
    private AuthService authService;
    @Autowired
    private UserRepository userRepository;
    // ...existing code...

    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private com.qrapp.util.JwtUtil jwtUtil;

    @PostMapping("/admin-login")
    public ResponseEntity<Map<String, Object>> adminLogin(@RequestBody Map<String, String> payload) {
        String username = payload.get("username");
        String password = payload.get("password");
        if (username == null || password == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Missing fields"));
        }
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password));
            User user = userRepository.findByUsernameAndStatusNot(username, "deleted").orElse(null);
            boolean wasDeleted = false;
            if (user == null && isValidEmail(username)) {
                user = userRepository.findByEmail(username).orElse(null);
                if (user != null && "deleted".equalsIgnoreCase(user.getStatus())) {
                    wasDeleted = true;
                }
            }
            if (wasDeleted || (user != null && "deleted".equalsIgnoreCase(user.getStatus()))) {
                return ResponseEntity.status(403)
                        .body(Map.of("success", false, "message", "Your account has been deleted by an administrator"));
            }
            if (user != null && "blocked".equalsIgnoreCase(user.getStatus())) {
                return ResponseEntity.status(403)
                        .body(Map.of("success", false, "message", "Your account has been blocked by an administrator"));
            }
            if (user == null || !"ROLE_ADMIN".equals(user.getRole())) {
                return ResponseEntity.status(403).body(Map.of("success", false, "message", "Not an admin user"));
            }
            String token = jwtUtil.generateToken(user.getUsername(), user.getRole());
            return ResponseEntity.ok(Map.of("success", true, "token", token));
        } catch (AuthenticationException ex) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "Invalid credentials"));
        }
    }

    private ResponseEntity<Map<String, Object>> badRequest(String message) {
        return ResponseEntity.badRequest().body(Map.of("success", false, "message", message));
    }

    private boolean isValidEmail(String email) {
        return email != null && !email.trim().isEmpty() &&
                email.matches(
                        "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.(com|net|org|co\\.in|in|edu|gov|io|me|info|biz)$");
    }

    private boolean isValidUsername(String username) {
        if (username == null)
            return false;
        String trimmed = username.trim();
        return trimmed.matches("^[a-zA-Z0-9_]{3,20}$");
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String email = request.get("email");
        String password = request.get("password");

        if (username == null || username.trim().isEmpty()) {
            return badRequest("Username is required");
        }
        if (!isValidUsername(username)) {
            return badRequest("Username must be 3-20 characters, letters, numbers, underscores only");
        }
        if (email == null || email.trim().isEmpty()) {
            return badRequest("Email is required");
        }
        if (!isValidEmail(email)) {
            return badRequest("Please enter a valid email address");
        }
        if (password == null || password.trim().isEmpty()) {
            return badRequest("Password is required");
        }
        if (password.length() < 6) {
            return badRequest("Password must be at least 6 characters long");
        }

        Map<String, Object> result = authService.register(username, email, password);
        return (Boolean) result.get("success") ? ResponseEntity.ok(result)
                : badRequest(result.get("message").toString());
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String password = request.get("password");

        if (email == null || email.trim().isEmpty()) {
            return badRequest("Email is required");
        }
        if (password == null || password.trim().isEmpty()) {
            return badRequest("Password is required");
        }

        // Try to find user by email
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            return badRequest("Invalid email or password");
        }
        if ("deleted".equalsIgnoreCase(user.getStatus())) {
            return ResponseEntity.status(403)
                    .body(Map.of("success", false, "message", "Your account has been deleted by an administrator"));
        }
        if ("blocked".equalsIgnoreCase(user.getStatus())) {
            return ResponseEntity.status(403)
                    .body(Map.of("success", false, "message", "Your account has been blocked by an administrator"));
        }
        try {
            // Authenticate using username (Spring Security default)
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(user.getUsername(), password));
            String token = jwtUtil.generateToken(user.getUsername(), user.getRole());
            if ("suspicious".equalsIgnoreCase(user.getStatus())) {
                suspiciousActivityService.logActivity(user, "login");
            }
            return ResponseEntity.ok(Map.of("success", true, "token", token));
        } catch (AuthenticationException ex) {
            return badRequest("Invalid email or password");
        }
    }

    @PostMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateToken(@RequestBody Map<String, String> request) {
        String token = request.get("token");

        if (token == null || token.trim().isEmpty()) {
            return badRequest("Token is required");
        }

        boolean isValid = authService.validateToken(token);
        String username = authService.getUsernameFromToken(token);
        return ResponseEntity.ok(Map.of(
                "success", isValid,
                "username", username != null ? username : ""));
    }

    @PostMapping("/profile")
    public ResponseEntity<Map<String, Object>> getUserProfile(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestHeader(value = "X-Log-Profile-View", required = false) String logHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401)
                    .body(Map.of("success", false, "message", "Authorization token is required"));
        }

        String token = authHeader.substring(7);
        boolean logProfileView = false;
        if (logHeader != null && (logHeader.equalsIgnoreCase("1") || logHeader.equalsIgnoreCase("true"))) {
            logProfileView = true;
        }
        Map<String, Object> result = authService.getUserProfile(token, logProfileView);
        return (Boolean) result.get("success") ? ResponseEntity.ok(result) : ResponseEntity.status(401).body(result);
    }

    @PostMapping("/profile/update")
    public ResponseEntity<Map<String, Object>> updateProfile(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody Map<String, String> request) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401)
                    .body(Map.of("success", false, "message", "Authorization token is required"));
        }

        String token = authHeader.substring(7);
        String username = request.get("username");
        String email = request.get("email");
        String newPassword = request.get("newPassword");

        if (email != null && !email.trim().isEmpty() && !isValidEmail(email)) {
            return badRequest("Please enter a valid email address");
        }

        Map<String, Object> result = authService.updateProfile(token, username, email, newPassword);
        return (Boolean) result.get("success") ? ResponseEntity.ok(result)
                : badRequest(result.get("message").toString());
    }

    @PostMapping("/check-email")
    public ResponseEntity<Map<String, Object>> checkEmailAvailability(@RequestBody Map<String, String> request) {
        String email = request.get("email");

        if (email == null || email.trim().isEmpty()) {
            return badRequest("Email is required");
        }
        if (!isValidEmail(email)) {
            return badRequest("Please enter a valid email address");
        }

        boolean isAvailable = authService.isEmailAvailable(email);
        return ResponseEntity.ok(Map.of(
                "available", isAvailable,
                "message", isAvailable ? "Email is available" : "Email is already registered"));
    }
}