package com.hdbank.auth_service.service;

import com.hdbank.auth_service.entity.AppUser;
import com.hdbank.auth_service.entity.RefreshToken;
import com.hdbank.auth_service.exception.InvalidRefreshTokenException;
import com.hdbank.auth_service.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${jwt.refresh-expiration}")
    private Long refreshTokenExpiration;

    @Transactional
    public RefreshToken createRefreshToken(
            AppUser user,
            String ipAddress,
            String userAgent
    ) {
        refreshTokenRepository.softDeleteByUserId(user.getId(), Instant.now(), "SYSTEM");

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(UUID.randomUUID().toString())
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusMillis(refreshTokenExpiration))
                .revoked(false)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .build();
        return refreshTokenRepository.save(refreshToken);
    }

    @Transactional(readOnly = true)
    public RefreshToken verifyRefreshToken(String token){
        RefreshToken refreshToken = refreshTokenRepository.findByTokenAndDeletedFalse(token)
                .orElseThrow(() -> new InvalidRefreshTokenException("Refresh token not found"));

        if(refreshToken.isRevoked()){
            throw new InvalidRefreshTokenException("Refresh token has been revoked");
        }
        if(refreshToken.getExpiresAt().isBefore(Instant.now())){
            throw new InvalidRefreshTokenException("Refresh token has expired");
        }
        return refreshToken;
    }

    @Transactional
    public void revokeRefreshToken(String token){
        RefreshToken refreshToken = refreshTokenRepository.findByTokenAndDeletedFalse(token)
                .orElseThrow(() -> new InvalidRefreshTokenException("Refresh token not found"));
        refreshToken.setRevoked(true);
        refreshToken.setRevokedAt(Instant.now());
        refreshTokenRepository.save(refreshToken);
    }

    @Transactional
    public void deleteExpiredTokens() {
        refreshTokenRepository.softDeleteExpiredTokens(
                Instant.now(),
                Instant.now(),
                "SYSTEM"
        );
    }

    @Transactional
    public void revokeAllByUserId(Long userId) {
        List<RefreshToken> tokens = refreshTokenRepository.findByUser_IdAndDeletedFalse(userId);

        if (tokens.isEmpty()) {
            return;
        }

        Instant now = Instant.now();

        tokens.forEach(token -> {
            token.setRevoked(true);
            token.setRevokedAt(now);
        });

        refreshTokenRepository.saveAll(tokens);
        refreshTokenRepository.softDeleteByUserId(userId, now, "SYSTEM");
        log.info("Revoked and soft-deleted {} tokens for user ID: {}", tokens.size(), userId);
    }


}
