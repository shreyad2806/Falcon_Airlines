package com.falcon.airlines.repository;

import com.falcon.airlines.entity.EmailVerificationToken;
import com.falcon.airlines.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for managing email verification tokens.
 */
@Repository
public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long>, JpaSpecificationExecutor<EmailVerificationToken> {

    Optional<EmailVerificationToken> findByToken(String token);

    List<EmailVerificationToken> findByUser(User user);
}
