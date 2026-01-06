package com.hdbank.auth_service.service;

import com.hdbank.auth_service.entity.AppUser;
import com.hdbank.auth_service.entity.RefreshToken;
import com.hdbank.auth_service.repository.RefreshTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    private AppUser testUser;

    @BeforeEach
    void setUp() {
        testUser = AppUser.builder()
                .id(1L)
                .username("testuser")
                .build();

        ReflectionTestUtils.setField(refreshTokenService, "refreshTokenExpiration", 604800000L); // 7 day
    }

    @Test
    void shouldCreateRefreshToken() {
        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RefreshToken refreshToken = refreshTokenService.createRefreshToken(
                testUser, "192.168.1.1", "Mozilla/5.0"
        );

        assertThat(refreshToken).isNotNull();
        assertThat(refreshToken.getUser()).isEqualTo(testUser);
        assertThat(refreshToken.getToken()).isNotNull();
        assertThat(refreshToken.getIpAddress()).isEqualTo("192.168.1.1");
        assertThat(refreshToken.getUserAgent()).isEqualTo("Mozilla/5.0");
        assertThat(refreshToken.isRevoked()).isFalse();
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void shouldVerifyValidRefreshToken() {
        RefreshToken validToken = RefreshToken.builder()
                .id(1L)
                .token("valid-token")
                .user(testUser)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .revoked(false)
                .build();

        when(refreshTokenRepository.findByTokenAndDeletedFalse("valid-token"))
                .thenReturn(Optional.of(validToken));

        RefreshToken result = refreshTokenService.verifyRefreshToken("valid-token");

        assertThat(result).isEqualTo(validToken);
    }

    @Test
    void shouldThrowExceptionForExpiredToken() {
        RefreshToken expiredToken = RefreshToken.builder()
                .id(1L)
                .token("expired-token")
                .user(testUser)
                .issuedAt(Instant.now().minusSeconds(7200))
                .expiresAt(Instant.now().minusSeconds(3600))
                .revoked(false)
                .build();

        when(refreshTokenRepository.findByTokenAndDeletedFalse("expired-token"))
                .thenReturn(Optional.of(expiredToken));

        assertThatThrownBy(() -> refreshTokenService.verifyRefreshToken("expired-token"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("expired");
    }

    @Test
    void shouldThrowExceptionForRevokedToken() {
        RefreshToken revokedToken = RefreshToken.builder()
                .id(1L)
                .token("revoked-token")
                .user(testUser)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .revoked(true)
                .revokedAt(Instant.now())
                .build();

        when(refreshTokenRepository.findByTokenAndDeletedFalse("revoked-token"))
                .thenReturn(Optional.of(revokedToken));

        assertThatThrownBy(() -> refreshTokenService.verifyRefreshToken("revoked-token"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("revoked");
    }

    @Test
    void shouldRevokeRefreshToken() {
        RefreshToken token = RefreshToken.builder()
                .id(1L)
                .token("token-to-revoke")
                .user(testUser)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .revoked(false)
                .build();

        when(refreshTokenRepository.findByTokenAndDeletedFalse("token-to-revoke"))
                .thenReturn(Optional.of(token));
        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        refreshTokenService.revokeRefreshToken("token-to-revoke");

        assertThat(token.isRevoked()).isTrue();
        assertThat(token.getRevokedAt()).isNotNull();
        verify(refreshTokenRepository).save(token);
    }
}