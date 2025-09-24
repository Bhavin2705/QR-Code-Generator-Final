package com.qrapp.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.qrapp.entity.QrCode;
import com.qrapp.service.CustomUserDetailsService;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
public class QrCodeRepositoryTest {
    @Autowired
    private QrCodeRepository qrCodeRepository;
    @Autowired
    private UserRepository userRepository;

    @Test
    void testSaveAndFindQrCode() {
        String uniqueEmail = "qruser_" + java.util.UUID.randomUUID() + "@example.com";
        com.qrapp.entity.User user = new com.qrapp.entity.User("qruser", uniqueEmail, "pass");
        user.setRole("user");
        user.setStatus("active");
        userRepository.save(user);
        QrCode qr = new QrCode("testQR", "generated", user);
        qrCodeRepository.save(qr);
        QrCode found = qrCodeRepository.findById(qr.getId()).orElse(null);
        assertNotNull(found);
        assertEquals("testQR", found.getInputText());
    }

    @AfterAll
    static void cleanTestUsers(
            @org.springframework.beans.factory.annotation.Autowired CustomUserDetailsService customUserDetailsService) {
        customUserDetailsService.deleteAllUsers();
    }
}
