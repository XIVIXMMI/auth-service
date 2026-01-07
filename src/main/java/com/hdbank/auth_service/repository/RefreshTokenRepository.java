package com.hdbank.auth_service.repository;

import com.hdbank.auth_service.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenAndDeletedFalse(String token);

    List<RefreshToken> findByUser_IdAndDeletedFalse(Long userId);

    @Modifying
    @Query("""
            UPDATE RefreshToken t\s
                SET t.deleted = true,\s
                    t.deletedAt = :deletedAt,\s
                    t.deletedBy = :deletedBy
            WHERE t.user.id = :userId\s
                AND t.deleted = false
           \s""")
    void softDeleteByUserId(@Param("userId") Long userId,
                            @Param("deletedAt") Instant deletedAt,
                            @Param("deletedBy") String deletedBy);

    @Modifying
    @Query("""
            UPDATE RefreshToken t\s
            SET t.deleted = true,\s
                t.deletedAt = :deletedAt,\s
                t.deletedBy = :deletedBy
            WHERE t.expiresAt < :expiresDate\s
                AND t.deleted = false
           \s""")
    void softDeleteExpiredTokens(@Param("expiresDate") Instant expiresDate,
                                @Param("deletedAt") Instant deletedAt,
                                @Param("deletedBy") String deletedBy);
}
