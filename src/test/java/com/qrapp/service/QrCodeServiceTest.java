package com.qrapp.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
public class QrCodeServiceTest {
    @Autowired
    private QrCodeService qrCodeService;

    @Test
    void testGenerateQrCodeImage() throws Exception {
        String base64 = qrCodeService.generateQrCodeImage("test");
        assertNotNull(base64);
        assertTrue(base64.length() > 0);
    }
}
