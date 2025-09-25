package com.qrapp.service;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.qrapp.entity.User;
import com.qrapp.repository.UserRepository;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class CustomUserDetailsServiceTest {
    @Autowired
    private CustomUserDetailsService service;
    @Autowired
    private UserRepository repo;

    @Test
    void testMarkAndUnmarkSuspicious() {
        String uniqueEmail = "markUser_" + java.util.UUID.randomUUID() + "@example.com";
        User user = new User("markUser", uniqueEmail, "pass");
        repo.save(user);
        assertTrue(service.markUserSuspicious(user.getId()));
        assertTrue(service.unmarkUserSuspicious(user.getId()));
    }

    @AfterAll
    static void cleanTestUsers(
            @org.springframework.beans.factory.annotation.Autowired CustomUserDetailsService customUserDetailsService) {
        customUserDetailsService.deleteAllUsers();
    }
}
