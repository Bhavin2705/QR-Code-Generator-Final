package com.qrapp.controller;

import java.util.List;

import com.qrapp.entity.User;
// Removed unused import
import com.qrapp.service.CustomUserDetailsService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    @Autowired
    private CustomUserDetailsService userDetailsService;
    // Removed unused AuthService

    // RBAC: Only allow admins
    private boolean isAdmin(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers(Authentication authentication) {
        if (!isAdmin(authentication)) {
            return ResponseEntity.status(403).body("Forbidden: Admins only");
        }
        List<User> users = userDetailsService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id, Authentication authentication) {
        if (!isAdmin(authentication)) {
            return ResponseEntity.status(403).body(java.util.Map.of("error", "Forbidden: Admins only"));
        }
        boolean deleted = userDetailsService.deleteUserById(id);
        if (deleted) {
            return ResponseEntity.ok(java.util.Map.of("success", true, "message", "User deleted"));
        } else {
            return ResponseEntity.status(404).body(java.util.Map.of("error", "User not found"));
        }
    }

    @DeleteMapping("/users")
    public ResponseEntity<?> deleteAllUsers(Authentication authentication) {
        if (!isAdmin(authentication)) {
            return ResponseEntity.status(403).body(java.util.Map.of("error", "Forbidden: Admins only"));
        }
        int deletedCount = userDetailsService.deleteAllUsers();
        return ResponseEntity.ok(java.util.Map.of("success", true, "deleted", deletedCount));
    }

    @PostMapping("/users/{id}/suspicious")
    public ResponseEntity<?> markUserSuspicious(@PathVariable Long id, Authentication authentication) {
        if (!isAdmin(authentication)) {
            return ResponseEntity.status(403).body(java.util.Map.of("error", "Forbidden: Admins only"));
        }
        boolean marked = userDetailsService.markUserSuspicious(id);
        if (marked) {
            return ResponseEntity.ok(java.util.Map.of("success", true, "message", "User marked as suspicious"));
        } else {
            return ResponseEntity.status(404).body(java.util.Map.of("error", "User not found"));
        }
    }

    @DeleteMapping("/users/{id}/suspicious")
    public ResponseEntity<?> unmarkUserSuspicious(@PathVariable Long id, Authentication authentication) {
        if (!isAdmin(authentication)) {
            return ResponseEntity.status(403).body(java.util.Map.of("error", "Forbidden: Admins only"));
        }
        boolean unmarked = userDetailsService.unmarkUserSuspicious(id);
        if (unmarked) {
            return ResponseEntity.ok(java.util.Map.of("success", true, "message", "User unmarked as suspicious"));
        } else {
            return ResponseEntity.status(404).body(java.util.Map.of("error", "User not found"));
        }
    }
}
