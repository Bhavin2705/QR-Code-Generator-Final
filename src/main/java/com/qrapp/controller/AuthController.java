package com.qrapp.controller;

import java.util.Map;

import com.qrapp.service.AuthService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
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
    private AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String email = request.get("email");
        String password = request.get("password");

        if (username == null || username.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Username is required"));
        }

        if (email == null || email.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Email is required"));
        }

        if (!isValidEmail(email)) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Please enter a valid email address"));
        }

        if (password == null || password.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Password is required"));
        }

        if (password.length() < 6) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Password must be at least 6 characters long"));
        }

        Map<String, Object> result = authService.register(username, email, password);

        if ((Boolean) result.get("success")) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String password = request.get("password");

        if (email == null || email.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Email is required"));
        }

        if (!isValidEmail(email)) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Please enter a valid email address"));
        }

        if (password == null || password.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Password is required"));
        }

        Map<String, Object> result = authService.login(email, password);

        if ((Boolean) result.get("success")) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }

    @PostMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateToken(@RequestBody Map<String, String> request) {
        String token = request.get("token");

        if (token == null || token.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Token is required"));
        }

        boolean isValid = authService.validateToken(token);
        String username = authService.getUsernameFromToken(token);

        return ResponseEntity.ok(Map.of(
                "success", isValid,
                "username", username != null ? username : ""));
    }

    @PostMapping("/profile")
    public ResponseEntity<Map<String, Object>> getUserProfile(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body(Map.of(
                    "success", false,
                    "message", "Authorization token is required"));
        }

        String token = authHeader.substring(7);
        Map<String, Object> result = authService.getUserProfile(token);

        if ((Boolean) result.get("success")) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.status(401).body(result);
        }
    }

    @PostMapping("/profile/update")
    public ResponseEntity<Map<String, Object>> updateProfile(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody Map<String, String> request) {

        System.out.println("Profile update request received");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            System.out.println("Missing or invalid Authorization header");
            return ResponseEntity.status(401).body(Map.of(
                    "success", false,
                    "message", "Authorization token is required"));
        }

        String token = authHeader.substring(7);
        String username = request.get("username");
        String email = request.get("email");
        String newPassword = request.get("newPassword");

        System.out.println("Update request - Username: " + username + ", Email: " + email +
                ", Has new password: " + (newPassword != null && !newPassword.isEmpty()));

        if (email != null && !email.trim().isEmpty() && !isValidEmail(email)) {
            System.out.println("Invalid email format: " + email);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Please enter a valid email address"));
        }

        Map<String, Object> result = authService.updateProfile(token, username, email, newPassword);
        System.out.println("Update result: " + result.get("success") + " - " + result.get("message"));

        if ((Boolean) result.get("success")) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }

    @PostMapping("/check-email")
    public ResponseEntity<Map<String, Object>> checkEmailAvailability(@RequestBody Map<String, String> request) {
        String email = request.get("email");

        if (email == null || email.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "available", false,
                    "message", "Email is required"));
        }

        if (!isValidEmail(email)) {
            return ResponseEntity.badRequest().body(Map.of(
                    "available", false,
                    "message", "Please enter a valid email address"));
        }

        boolean isAvailable = authService.isEmailAvailable(email);

        return ResponseEntity.ok(Map.of(
                "available", isAvailable,
                "message", isAvailable ? "Email is available" : "Email is already registered"));
    }

    private boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }

        String emailRegex = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";
        return email.matches(emailRegex);
    }
}
