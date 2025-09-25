package com.qrapp.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
public class SuspiciousActivityControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void testGetSuspiciousActivityLogsForbiddenForNonAdmin() throws Exception {
        mockMvc.perform(get("/api/admin/suspicious-activity"))
                .andExpect(status().isForbidden());
    }
}
