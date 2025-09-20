package com.qrapp.controller;

import com.qrapp.entity.SuspiciousActivity;
import com.qrapp.repository.SuspiciousActivityRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class SuspiciousActivityController {
    @Autowired
    private SuspiciousActivityRepository suspiciousActivityRepository;

    @GetMapping("/suspicious-activity")
    public ResponseEntity<List<SuspiciousActivity>> getSuspiciousActivityLogs(Authentication authentication) {
        // RBAC: Only allow admins
        if (authentication == null
                || authentication.getAuthorities().stream().noneMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            return ResponseEntity.status(403).build();
        }
        List<SuspiciousActivity> logs = suspiciousActivityRepository.findAll();
        return ResponseEntity.ok(logs);
    }
}
