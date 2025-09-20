package com.qrapp.service;

import com.qrapp.entity.SuspiciousActivity;
import com.qrapp.entity.User;
import com.qrapp.repository.SuspiciousActivityRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SuspiciousActivityService {
    @Autowired
    private SuspiciousActivityRepository suspiciousActivityRepository;

    public void logActivity(User user, String action) {
        if (user != null && "suspicious".equalsIgnoreCase(user.getStatus())) {
            Long userId = user.getId() != null ? user.getId() : -1L;
            String username = user.getUsername() != null ? user.getUsername() : "N/A";
            String safeAction = action != null ? action : "N/A";
            SuspiciousActivity activity = new SuspiciousActivity(userId, username, safeAction);
            suspiciousActivityRepository.save(activity);
        }
    }
}
