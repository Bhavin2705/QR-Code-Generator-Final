package com.qrapp.controller;

import java.util.List;

import com.qrapp.entity.SuspiciousActivity;
import com.qrapp.repository.SuspiciousActivityRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class SuspiciousActivityController {
    @Autowired
    private SuspiciousActivityRepository suspiciousActivityRepository;

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/suspicious-activity")
    public ResponseEntity<List<SuspiciousActivity>> getSuspiciousActivityLogs() {
        List<SuspiciousActivity> logs = suspiciousActivityRepository.findAll();
        return ResponseEntity.ok(logs);
    }
}
