package com.qrapp.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class JwtUtilTest {
    @Autowired
    private JwtUtil jwtUtil;

    @Test
    void testGenerateAndValidateToken() {
        String token = jwtUtil.generateToken("user1", "user");
        assertNotNull(token);
        String username = jwtUtil.extractUsername(token);
        assertEquals("user1", username);
        assertTrue(jwtUtil.validateToken(token, "user1"));
    }
}
