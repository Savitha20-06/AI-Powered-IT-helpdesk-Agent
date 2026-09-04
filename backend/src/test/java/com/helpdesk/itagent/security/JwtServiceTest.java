package com.helpdesk.itagent.security;

import com.helpdesk.itagent.config.AppProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        AppProperties properties = new AppProperties();
        properties.getJwt().setSecret("jwt-test-secret-that-is-long-enough-123456");
        properties.getJwt().setExpirationMs(60_000);
        jwtService = new JwtService(properties);
    }

    @Test
    void generatesTokenWithEmailAndClaims() {
        String token = jwtService.generateToken("employee@example.com", Map.of("role", "EMPLOYEE"));

        assertThat(jwtService.extractEmail(token)).isEqualTo("employee@example.com");
        assertThat(jwtService.isValid(token)).isTrue();
    }

    @Test
    void returnsFalseForMalformedToken() {
        assertThat(jwtService.isValid("not-a-jwt")).isFalse();
    }

    @Test
    void returnsFalseForExpiredToken() {
        AppProperties properties = new AppProperties();
        properties.getJwt().setSecret("jwt-test-secret-that-is-long-enough-123456");
        properties.getJwt().setExpirationMs(-1);
        JwtService expiredService = new JwtService(properties);

        String token = expiredService.generateToken("employee@example.com", Map.of());

        assertThat(expiredService.isValid(token)).isFalse();
    }
}
