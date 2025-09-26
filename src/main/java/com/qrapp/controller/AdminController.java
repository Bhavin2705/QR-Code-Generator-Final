package com.qrapp.controller;

import java.util.List;

import com.qrapp.entity.User;
// Removed unused import
import com.qrapp.service.CustomUserDetailsService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers() {
        List<User> users = userDetailsService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        boolean deleted = userDetailsService.deleteUserById(id);
        if (deleted) {
            return ResponseEntity.ok(java.util.Map.of("success", true, "message", "User deleted"));
        } else {
            return ResponseEntity.status(404).body(java.util.Map.of("error", "User not found"));
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/users/{id}/block")
    public ResponseEntity<?> blockUser(@PathVariable Long id) {
        boolean blocked = userDetailsService.blockUser(id);
        if (blocked) {
            return ResponseEntity.ok(java.util.Map.of("success", true, "message", "User blocked"));
        } else {
            return ResponseEntity.status(404).body(java.util.Map.of("error", "User not found or cannot block admin"));
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/users/{id}/unblock")
    public ResponseEntity<?> unblockUser(@PathVariable Long id) {
        boolean unblocked = userDetailsService.unblockUser(id);
        if (unblocked) {
            return ResponseEntity.ok(java.util.Map.of("success", true, "message", "User unblocked"));
        } else {
            return ResponseEntity.status(404).body(java.util.Map.of("error", "User not found"));
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/users")
    public ResponseEntity<?> deleteAllUsers() {
        int deletedCount = userDetailsService.deleteAllUsers();
        return ResponseEntity.ok(java.util.Map.of("success", true, "deleted", deletedCount));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/users/{id}/suspicious")
    public ResponseEntity<?> markUserSuspicious(@PathVariable Long id) {
        boolean marked = userDetailsService.markUserSuspicious(id);
        if (marked) {
            return ResponseEntity.ok(java.util.Map.of("success", true, "message", "User marked as suspicious"));
        } else {
            return ResponseEntity.status(404).body(java.util.Map.of("error", "User not found"));
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/users/{id}/suspicious")
    public ResponseEntity<?> unmarkUserSuspicious(@PathVariable Long id) {
        boolean unmarked = userDetailsService.unmarkUserSuspicious(id);
        if (unmarked) {
            return ResponseEntity.ok(java.util.Map.of("success", true, "message", "User unmarked as suspicious"));
        } else {
            return ResponseEntity.status(404).body(java.util.Map.of("error", "User not found"));
        }
    }
}
