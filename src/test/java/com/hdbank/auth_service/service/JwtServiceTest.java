package com.hdbank.auth_service.service;

import com.hdbank.auth_service.entity.AppRole;
import com.hdbank.auth_service.entity.AppUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class JwtServiceTest {

    @Autowired
    private JwtService jwtService;

    private AppUser testUser;

    @BeforeEach
    void setUp() {
        AppRole role = AppRole.builder()
                .id(1L)
                .name("ROLE_USER")
                .build();

        testUser = AppUser.builder()
                .id(1L)
                .username("testuser")
                .passwordHash("hashedpassword")
                .fullName("Test User")
                .enabled(true)
                .roles(Set.of(role))
                .build();
    }

    @Test
    void shouldGenerateValidAccessToken() {
        String token = jwtService.generateAccessToken(testUser);

        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
        assertThat(token.split("\\.")).hasSize(3); // JWT has 3 parts
    }

    @Test
    void shouldExtractUsernameFromToken() {
        String token = jwtService.generateAccessToken(testUser);

        String username = jwtService.extractUsername(token);

        assertThat(username).isEqualTo("testuser");
    }

    @Test
    void shouldExtractUserIdFromToken() {
        String token = jwtService.generateAccessToken(testUser);

        Long userId = jwtService.extractUserId(token);

        assertThat(userId).isEqualTo(1L);
    }

    @Test
    void shouldValidateValidToken() {
        String token = jwtService.generateAccessToken(testUser);

        boolean isValid = jwtService.isTokenValid(token);

        assertThat(isValid).isTrue();
    }

    @Test
    void shouldReturnFalseForInvalidToken() {
        String invalidToken = "invalid.token.here";

        boolean isValid = jwtService.isTokenValid(invalidToken);

        assertThat(isValid).isFalse();
    }

    @Test
    void shouldReturnFalseForMalformedToken() {
        String malformedToken = "not-a-jwt-token";

        boolean isValid = jwtService.isTokenValid(malformedToken);

        assertThat(isValid).isFalse();
    }
}