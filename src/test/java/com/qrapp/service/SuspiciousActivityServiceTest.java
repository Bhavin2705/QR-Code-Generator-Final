package com.qrapp.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import com.qrapp.entity.User;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class SuspiciousActivityServiceTest {
    @Autowired
    private SuspiciousActivityService suspiciousActivityService;

    @Test
    void testLogActivity() {
        User user = new User("susUser", "sus@example.com", "pass");
        user.setStatus("suspicious");
        assertDoesNotThrow(() -> suspiciousActivityService.logActivity(user, "login"));
    }
}
