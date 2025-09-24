package com.qrapp.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.qrapp.entity.User;
import com.qrapp.service.CustomUserDetailsService;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
public class UserRepositoryTest {
    @Autowired
    private UserRepository userRepository;

    @Test
    void testSaveAndFindUser() {
        String uniqueEmail = "repoUser_" + java.util.UUID.randomUUID() + "@example.com";
        User user = new User("repoUser", uniqueEmail, "password");
        userRepository.save(user);
        User found = userRepository.findByEmail(uniqueEmail).orElse(null);
        assertNotNull(found);
        assertEquals("repoUser", found.getUsername());
    }

    @AfterAll
    static void cleanTestUsers(
            @org.springframework.beans.factory.annotation.Autowired CustomUserDetailsService customUserDetailsService) {
        customUserDetailsService.deleteAllUsers();
    }
}
