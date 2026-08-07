package com.falcon.airlines.entity;

import com.falcon.airlines.common.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

import java.time.Instant;

/**
 * One-time token used to authorise a password reset.
 * <p>
 * The token is deleted once it has been used or after it expires.
 */
@Getter
@Setter
@Entity
@Table(
        name = "password_reset_tokens",
        indexes = {
                @Index(name = "idx_password_reset_tokens_user_id", columnList = "user_id"),
                @Index(name = "idx_password_reset_tokens_expires_at", columnList = "expires_at")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_password_reset_tokens_token", columnNames = {"token"})
        }
)
@SQLRestriction("is_deleted = false")
public class PasswordResetToken extends BaseEntity {

    @NotBlank
    @Column(name = "token", length = 512, nullable = false, unique = true)
    private String token;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotNull
    @Future
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;
}
