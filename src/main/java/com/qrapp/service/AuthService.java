package com.qrapp.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.qrapp.entity.User;
import com.qrapp.repository.UserRepository;
import com.qrapp.util.JwtUtil;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    public Map<String, Object> register(String username, String email, String password) {
        Map<String, Object> response = new HashMap<>();

        if (userRepository.existsByEmail(email)) {
            response.put("success", false);
            response.put("message", "Email already exists");
            return response;
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));

        try {
            userRepository.save(user);
            String token = jwtUtil.generateToken(username);

            response.put("success", true);
            response.put("message", "User registered successfully");
            response.put("token", token);
            response.put("username", username);
            return response;
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Registration failed: " + e.getMessage());
            return response;
        }
    }

    public Map<String, Object> login(String email, String password) {
        Map<String, Object> response = new HashMap<>();

        Optional<User> userOptional = userRepository.findByEmail(email);

        if (userOptional.isEmpty()) {
            response.put("success", false);
            response.put("message", "Invalid email or password");
            return response;
        }

        User user = userOptional.get();

        if (!passwordEncoder.matches(password, user.getPassword())) {
            response.put("success", false);
            response.put("message", "Invalid email or password");
            return response;
        }

        String token = jwtUtil.generateToken(user.getUsername());

        response.put("success", true);
        response.put("message", "Login successful");
        response.put("token", token);
        response.put("username", user.getUsername());
        response.put("email", user.getEmail());
        return response;
    }

    public boolean validateToken(String token) {
        try {
            String username = jwtUtil.extractUsername(token);
            return jwtUtil.validateToken(token, username);
        } catch (Exception e) {
            return false;
        }
    }

    public String getUsernameFromToken(String token) {
        try {
            return jwtUtil.extractUsername(token);
        } catch (Exception e) {
            return null;
        }
    }

    public User getUserFromToken(String token) {
        try {
            String username = jwtUtil.extractUsername(token);
            return userRepository.findByUsername(username).orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    public Map<String, Object> getUserProfile(String token) {
        Map<String, Object> response = new HashMap<>();

        try {
            if (!validateToken(token)) {
                response.put("success", false);
                response.put("message", "Invalid or expired token");
                return response;
            }

            User user = getUserFromToken(token);
            if (user == null) {
                response.put("success", false);
                response.put("message", "Invalid token or user not found");
                return response;
            }
            response.put("success", true);
            response.put("username", user.getUsername());
            response.put("email", user.getEmail());
            response.put("createdAt", user.getCreatedAt());
            response.put("updatedAt", user.getUpdatedAt());

            return response;
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error retrieving user profile: " + e.getMessage());
            return response;
        }
    }

    public boolean isEmailAvailable(String email) {
        try {
            Optional<User> userOptional = userRepository.findByEmail(email);
            return userOptional.isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    public Map<String, Object> updateProfile(String token, String username, String email, String newPassword) {
        Map<String, Object> response = new HashMap<>();

        try {
            String tokenUsername = jwtUtil.extractUsername(token);
            if (tokenUsername == null) {
                response.put("success", false);
                response.put("message", "Invalid token");
                return response;
            }

            Optional<User> userOptional = userRepository.findByUsername(tokenUsername);
            if (userOptional.isEmpty()) {
                response.put("success", false);
                response.put("message", "User not found");
                return response;
            }

            User user = userOptional.get();
            boolean updated = false;

            if (username != null && !username.trim().isEmpty() && !username.equals(user.getUsername())) {
                user.setUsername(username.trim());
                updated = true;
            }

            if (email != null && !email.trim().isEmpty() && !email.equals(user.getEmail())) {
                Optional<User> existingUser = userRepository.findByEmail(email);
                if (existingUser.isPresent() && !existingUser.get().getId().equals(user.getId())) {
                    response.put("success", false);
                    response.put("message", "Email is already registered by another user");
                    return response;
                }
                user.setEmail(email.trim());
                updated = true;
            }

            if (newPassword != null && !newPassword.trim().isEmpty()) {
                if (newPassword.length() < 6) {
                    response.put("success", false);
                    response.put("message", "New password must be at least 6 characters long");
                    return response;
                }
                user.setPassword(passwordEncoder.encode(newPassword));
                updated = true;
            }

            if (updated) {
                userRepository.save(user);
                response.put("success", true);
                response.put("message", "Profile updated successfully");
                response.put("username", user.getUsername());
                response.put("email", user.getEmail());
            } else {
                response.put("success", true);
                response.put("message", "No changes to update");
                response.put("username", user.getUsername());
                response.put("email", user.getEmail());
            }

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error updating profile: " + e.getMessage());
        }

        return response;
    }
}