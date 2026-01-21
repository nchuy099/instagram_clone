package com.nchuy099.mini_instagram.token.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.nchuy099.mini_instagram.token.entity.BlackListToken;
import com.nchuy099.mini_instagram.token.entity.ResetPasswordToken;

@Repository
public interface ResetPasswordTokenRepository extends JpaRepository<ResetPasswordToken, UUID> {
    Optional<ResetPasswordToken> findByJti(UUID jti);
}
