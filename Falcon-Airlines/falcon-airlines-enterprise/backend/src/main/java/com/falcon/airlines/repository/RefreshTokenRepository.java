package com.falcon.airlines.repository;

import com.falcon.airlines.entity.RefreshToken;
import com.falcon.airlines.entity.User;
import com.falcon.airlines.enums.TokenStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repository for managing refresh token persistence and lifecycle.
 */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long>, JpaSpecificationExecutor<RefreshToken> {

    Optional<RefreshToken> findByToken(String token);

    List<RefreshToken> findByUser(User user);

    List<RefreshToken> findByUserAndStatusAndExpiresAtAfter(User user, TokenStatus status, Instant now);

    long countByUserAndStatusAndExpiresAtAfter(User user, TokenStatus status, Instant now);

    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.status = :status, rt.revokedAt = :revokedAt WHERE rt.user = :user AND rt.status = 'ACTIVE'")
    int revokeActiveTokensForUser(@Param("user") User user,
                                  @Param("status") TokenStatus status,
                                  @Param("revokedAt") Instant revokedAt);

    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.status = 'EXPIRED' WHERE rt.status = 'ACTIVE' AND rt.expiresAt < :now")
    int markExpiredTokens(@Param("now") Instant now);

    void deleteByExpiresAtBefore(Instant now);

    void deleteByUser(User user);
}
