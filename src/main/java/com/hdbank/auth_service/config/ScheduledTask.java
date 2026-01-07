package com.hdbank.auth_service.config;

import com.hdbank.auth_service.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ScheduledTask {

    private final RefreshTokenService refreshTokenService;

    @Scheduled(cron = "0 0 2 * * *") // 2AM Daily
    public void cleanupExpiredTokens() {
        log.info("[CRON] Cleanup expired tokens");
        refreshTokenService.deleteExpiredTokens();
    }
}
