package com.qrapp.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JwtConfig {

    @Value("${jwt.secret:mySecretKey}")
    private String secret;

    @Value("${jwt.expiration:86400000}")
    private Long expiration;

    public String getSecret() {
        return secret;
    }

    public Long getExpiration() {
        return expiration;
    }
}
