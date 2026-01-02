package com.hdbank.auth_service.repository;

import com.hdbank.auth_service.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenAndIsDeletedFalse(String token);

    void deleteByUserIdAndIsDeletedFalse(Long userId);

    void deleteByExpiresAtBeforeAndIsDeletedFalse(Instant expiresDate);
}
