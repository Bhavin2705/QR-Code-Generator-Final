package com.qrapp.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.qrapp.service.CustomUserDetailsService;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class AuthControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void testRegisterAndLogin() throws Exception {
        String uniqueEmail = "testuser_" + java.util.UUID.randomUUID() + "@example.com";
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"testuser\",\"email\":\"" + uniqueEmail + "\",\"password\":\"testpass123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + uniqueEmail + "\",\"password\":\"testpass123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @AfterAll
    static void cleanTestUsers(
            @org.springframework.beans.factory.annotation.Autowired CustomUserDetailsService customUserDetailsService) {
        customUserDetailsService.deleteAllUsers();
    }
}
